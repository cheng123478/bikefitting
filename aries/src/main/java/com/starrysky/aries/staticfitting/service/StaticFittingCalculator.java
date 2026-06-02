package com.starrysky.aries.staticfitting.service;

import com.starrysky.aries.util.BikeType;

/**
 * 静态Fitting数学引擎
 * 本类封装了从基础选型到高级生物力学修正的核心算法。
 * 算法逻辑参考了经典的LeMond法则、KOPS准则以及针对人体柔韧性的动态偏移补偿。
 *
 * @author adi
 * @version 1.1
 * @since 2026/2/5 9:06
 */
public class StaticFittingCalculator {

    // --- LEVEL 0: 数据质量校验 ---

    /**
     * 校验人体比例是否在生物力学常模范围内。
     *
     * @param spanHeight 跨高
     * @param height     身高
     * @param wingspan  臂展
     * @return 0: 正常, 1: 跨高比例极高, 2: 跨高比例极低, 3: 猿臂比异常
     */
    public static int validateHumanProportions(Integer spanHeight, Integer height, Integer wingspan) {
        if (spanHeight == null || height == null || height <= 0)
            return 0;

        double spanRatio = (double) spanHeight / height;
        if (spanRatio > FittingConstants.SPAN_HEIGHT_RATIO_MAX)
            return 1;
        if (spanRatio < FittingConstants.SPAN_HEIGHT_RATIO_MIN)
            return 2;

        if (wingspan != null) {
            double apeIndex = (double) wingspan / height;
            if (apeIndex > FittingConstants.APE_INDEX_MAX || apeIndex < FittingConstants.APE_INDEX_MIN)
                return 3;
        }
        return 0;
    }

    // --- LEVEL 1: 基础选型算法 (Sizing) ---

    /**
     * 根据跨高和车型计算基础座垫高度
     *
     * @param spanHeight 跨高
     * @param bikeType   车型
     * @return 基础座垫高度
     */
    public static int calcBaseSeatHeight(int spanHeight, int bikeType) {
        double k = BikeType.getByCode(bikeType).seatHeightCoefficient;
        return (int) Math.round(spanHeight * k);
    }

    /**
     * 计算车架基础堆高 (Stack)
     * 高决定了车辆的前端基础高度，影响骑行者的重心分布和操控稳定性
     *
     * @param spanHeight 跨高
     * @param bikeType   车型
     * @return 车架基础堆高
     */
    public static int calcBaseStack(int spanHeight, int bikeType) {
        double stackK;
        if (bikeType == BikeType.CITY_BIKE.code) {
            stackK = FittingConstants.CITY_STACK_K;
        } else if (bikeType == BikeType.TT_BIKE.code) {
            stackK = FittingConstants.TT_STACK_K;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            stackK = FittingConstants.MTB_STACK_K;
        } else if (bikeType == BikeType.GRAVEL_BIKE.code) {
            stackK = 0.71; // Gravel stack coefficient
        } else {
            stackK = FittingConstants.ROAD_STACK_K;
        }
        return (int) Math.round(spanHeight * stackK);
    }

    // --- LEVEL 2: 规格适配算法 (Ergonomics) ---

    /**
     * 计算建议曲柄长度
     * 合适的曲柄长度能确保踩踏圆周运动中，膝关节与髋关节的角度在安全且高效的范围内波动。
     * TT车: 建议比标准值短 2.5mm。短曲柄能打开髋关节角度，减少趴姿时大腿撞击腹部的感觉。
     *
     * @param spanHeight 跨高 (mm)
     * @param bikeType   单车类型
     * @return 建议曲柄长度 (mm)，取整至最近的 2.5mm (如 170, 172.5)
     */
    public static int calcCrankLength(int spanHeight, int bikeType) {
        double raw = spanHeight * FittingConstants.CRANK_RATIO;
        if (bikeType == BikeType.TT_BIKE.code) {
            raw -= 2.5;
        }
        return (int) (Math.round(raw / 2.5) * 2.5);
    }

    /**
     * 计算建议座垫宽度。
     * 旨在支撑坐骨结节 (Ischial Tuberosity)，避免压力转移至会阴部软组织。
     * 城市车(5): +40mm。直立骑行时，坐骨几乎垂直压在座垫上，需要极宽的支撑。
     * 山地(2): +30mm。
     * Road/TT(1,4): +20mm。激进前倾姿态，接触点变窄
     *
     * @param ischialDistance 坐骨间距 (mm)
     * @param bikeType        单车类型 (1:公路, 2:山地/Gravel)
     * @return 建议座垫宽度 (mm)
     */
    public static int calcSaddleWidth(int ischialDistance, int bikeType) {
        int offset;
        if (bikeType == BikeType.CITY_BIKE.code) {
            offset = 40;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            offset = 30;
        } else if (bikeType == BikeType.GRAVEL_BIKE.code) {
            offset = 25;
        } else {
            offset = 20;
        }
        return ischialDistance + offset;
    }

    // --- LEVEL 3: 高级修正算法 ---

    /**
     * 根据股骨比例 (Femur Ratio) 计算座垫建议前后偏移量。
     * 正常比例为 0.45 - 0.47。
     *
     * @param femurRatio 股骨比 (大腿长 / 跨高)
     * @return 建议描述
     */
    public static String calcSaddleSetbackAdvice(double femurRatio) {
        if (femurRatio > 0.47) {
            return "大腿部骨骼比例偏长，建议座垫向后移动 5-10mm 以平衡中心并保护膝盖。";
        } else if (femurRatio < 0.45) {
            return "大腿部骨骼比例偏短，建议座垫向前移动 5-10mm。";
        }
        return "比例正常，座垫保持居中即可。";
    }

    /**
     * 计算建议座垫后移量 (Setback) 数值。
     * 根据股骨比例动态调整系数：长股骨(>0.47)用0.35，正常用0.32，短股骨(<0.45)用0.29。
     *
     * @param thighLength 大腿长 (mm)
     * @param femurRatio  股骨比
     * @return 建议座垫后移量 (mm)
     */
    public static int calcSetback(int thighLength, double femurRatio) {
        double k;
        if (femurRatio > 0.47) {
            k = FittingConstants.SETBACK_K_LONG_FEMUR;
        } else if (femurRatio < 0.45) {
            k = FittingConstants.SETBACK_K_SHORT_FEMUR;
        } else {
            k = FittingConstants.SETBACK_K_STANDARD;
        }
        return (int) Math.round(thighLength * k);
    }

    /**
     * 根据胫骨长度比例 (Tibia Ratio) 计算座高修正偏移量。
     * 小腿过长的人更容易在踩踏底部出现垫脚尖（跟腱过度负荷），通过微降座高给予保护。
     *
     * @param calf       小腿长 (mm)
     * @param spanHeight 跨高 (mm)
     * @return 座高修正值 (mm)，负值代表需要调低座高
     */
    public static int calcTibiaCorrection(int calf, int spanHeight) {
        if (calf <= 0 || spanHeight <= 0)
            return 0;
        double ratio = (double) calf / spanHeight;
        return (ratio > 0.52) ? -5 : 0;
    }

    /**
     * 计算有效空间前伸量 (Reach Plus)。
     * 决定了座垫鼻尖到车把中心的空间距离，结合了躯干长度与胸椎灵活性修正。
     * TT(4): 0.475。肘垫位置极靠前，身体延伸最大。
     * City(5): 0.380。追求直立，把手离身体很近。
     * Road(1): 0.445。
     *
     * @param trunk         躯干长度 (mm)
     * @param arms          手臂总长 (mm)
     * @param thoracicScore 胸椎灵活性综合得分 (0-100)
     * @param bikeType      单车类型
     * @return 理想空间前伸量 Reach+ (mm)
     */
    public static double calcReachPlus(int trunk, int arms, int thoracicScore, int bikeType) {
        double baseReach = (trunk + arms) / FittingConstants.REACH_PLUS_BASE_DIVIDER;

        double typeFactor;
        if (bikeType == BikeType.TT_BIKE.code) {
            typeFactor = FittingConstants.REACH_PLUS_TT_FACTOR;
        } else if (bikeType == BikeType.CITY_BIKE.code) {
            typeFactor = FittingConstants.REACH_PLUS_CITY_FACTOR;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            typeFactor = FittingConstants.REACH_PLUS_MTB_FACTOR;
        } else if (bikeType == BikeType.GRAVEL_BIKE.code) {
            typeFactor = 0.98; // Gravel reach factor
        } else {
            typeFactor = FittingConstants.REACH_PLUS_ROAD_FACTOR;
        }

        double flexFactor = 0.92 + (thoracicScore / 100.0 * 0.16);
        return Math.round(baseReach * typeFactor * flexFactor * 10.0) / 10.0;
    }

    /**
     * 计算建议车把落差 (Drop)。
     * 即座垫高度与车把高度的高度差。受身高及肩部（举臂）灵活性限制。
     * City(5): 负落差 (e.g., -0.03)，即车把高于座垫。
     * TT(4): 0.060，极致气动。
     * MTB(2): 0.020，较小落差。
     *
     * @param height        身高 (mm)
     * @param armRaiseAngle 举臂角度 (度)
     * @param thoracicScore 胸椎灵活性得分 (0-100)
     * @param bikeType      单车类型
     * @return 建议落差值 (mm)
     */
    public static double calcHandlebarDrop(int height, double armRaiseAngle, Integer thoracicScore, int bikeType) {
        double dropK;
        if (bikeType == BikeType.TT_BIKE.code) {
            dropK = FittingConstants.DROP_K_TT;
        } else if (bikeType == BikeType.CITY_BIKE.code) {
            dropK = FittingConstants.DROP_K_CITY;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            dropK = FittingConstants.DROP_K_MTB;
        } else {
            dropK = FittingConstants.DROP_K_ROAD;
        }
        double drop = height * dropK;
        if (armRaiseAngle > 0 && armRaiseAngle < 155) {
            if (bikeType != BikeType.CITY_BIKE.code)
                drop *= 0.7; // Not CITY_BIKE
        }
        if (thoracicScore != null && thoracicScore < 60)
            drop *= 0.85;
        return Math.round(drop * 10.0) / 10.0;
    }

    /**
     * 计算前伸偏移量 (基于把立与角度)
     *
     * @param stemLength 把立长度 (mm)
     * @param stemAngle  把立角度 (度)
     * @return 前伸量贡献值 (mm)
     */
    public static double calculateReachOffset(int stemLength, double stemAngle) {
        return stemLength * Math.cos(Math.toRadians(Math.abs(stemAngle)));
    }

    public static Double calculateBmi(Integer heightMm, Double weightKg) {
        if (heightMm == null || heightMm <= 0 || weightKg == null || weightKg <= 0) {
            return null;
        }
        double heightM = heightMm / 1000.0;
        double bmi = weightKg / (heightM * heightM);
        return Math.round(bmi * 10.0) / 10.0;
    }

    public static Double calculateWaistToHeightRatio(Integer heightMm, Integer waistMm) {
        if (heightMm == null || heightMm <= 0 || waistMm == null || waistMm <= 0) {
            return null;
        }
        double ratio = (double) waistMm / heightMm;
        return Math.round(ratio * 1000.0) / 1000.0;
    }

    public static boolean hasAbdominalClearanceNeed(Double bmi, Double waistToHeightRatio) {
        return bmi != null
                && waistToHeightRatio != null
                && bmi >= FittingConstants.BMI_OBESITY_THRESHOLD
                && waistToHeightRatio >= FittingConstants.WAIST_HEIGHT_RATIO_RISK_THRESHOLD;
    }

    public static boolean isSevereAbdominalClearanceNeed(Double waistToHeightRatio) {
        return waistToHeightRatio != null
                && waistToHeightRatio >= FittingConstants.ABDOMINAL_SEVERE_WAIST_HEIGHT_RATIO;
    }

    public static int calcAbdominalReachReduction(Double bmi, Double waistToHeightRatio) {
        if (!hasAbdominalClearanceNeed(bmi, waistToHeightRatio)) {
            return 0;
        }
        return isSevereAbdominalClearanceNeed(waistToHeightRatio)
                ? FittingConstants.ABDOMINAL_REACH_REDUCTION_SEVERE
                : FittingConstants.ABDOMINAL_REACH_REDUCTION_MILD;
    }

    public static int calcAbdominalDropReduction(Double bmi, Double waistToHeightRatio) {
        if (!hasAbdominalClearanceNeed(bmi, waistToHeightRatio)) {
            return 0;
        }
        return isSevereAbdominalClearanceNeed(waistToHeightRatio)
                ? FittingConstants.ABDOMINAL_DROP_REDUCTION_SEVERE
                : FittingConstants.ABDOMINAL_DROP_REDUCTION_MILD;
    }
}
