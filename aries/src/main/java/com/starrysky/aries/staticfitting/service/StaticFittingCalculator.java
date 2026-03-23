package com.starrysky.aries.staticfitting.service;

import com.starrysky.aries.util.BikeType;

/**
 * 静态Fitting数学引擎
 * 本类封装了从基础选型到高级生物力学修正的核心算法。
 * 算法逻辑参考了经典的LeMond法则、KOPS准则以及针对人体柔韧性的动态偏移补偿。
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/5 9:06
 */
public class StaticFittingCalculator {
    // --- LEVEL 1: 基础选型算法 (Sizing) ---

    /**
     * 根据跨高和车型计算基础座垫高度。
     * 采用经典的 LeMond 公式 (跨高 * 0.883)，旨在使膝关节在踏板下死点达到最优伸展角度（约 145°-155°）。
     * TT车(4): 0.885。立管角度陡，且为了气动需腿部伸展更直。
     * 城市车(5): 0.870。需兼顾红绿灯停车时的脚掌触地安全性。
     * 公路(1): 0.883。经典 LeMond。
     * 山地/Gravel(2,3): 0.880。应对颠簸路面，略微降低重心。
     *
     * @param spanHeight 跨高 (mm)
     * @param bikeType   单车类型 (1:公路, 2:山地/Gravel)
     * @return 基础座高建议 (mm)，从五通中心至座垫顶端
     */
    public static int calcBaseSeatHeight(int spanHeight, int bikeType) {
        double k;
        if (bikeType == BikeType.TT_BIKE.code) {
            k = 0.885;
        } else if (bikeType == BikeType.CITY_BIKE.code) {
            k = 0.870;
        } else if (bikeType == BikeType.ROAD_BIKE.code) {
            k = 0.883;
        } else {
            // Mountain & Gravel
            k = 0.880;
        }
        return (int) Math.round(spanHeight * k);
    }

    /**
     * 计算车架基础堆高 (Stack)。
     * 堆高决定了车辆的前端基础高度，影响骑行者的重心分布和操控稳定性。
     * City: 0.75 (高车头，舒适)
     * TT: 0.62 (低车头，气动)
     * Road: 0.69 (标准)
     *
     * @param spanHeight 跨高 (mm)
     * @param bikeType   单车类型
     * @return 理论车架 Stack 值 (mm)
     */
    public static int calcBaseStack(int spanHeight, int bikeType) {
        double stackK;
        if (bikeType == BikeType.CITY_BIKE.code) {
            stackK = 0.75;
        } else if (bikeType == BikeType.TT_BIKE.code) {
            stackK = 0.62;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            stackK = 0.70; // 考虑避震行程修正
        } else {
            // Road & Gravel
            stackK = 0.69;
        }
        return (int) Math.round(spanHeight * stackK);
    }

    // --- LEVEL 2: 规格适配算法 (Ergonomics) ---

    /**
     * 计算推荐的曲柄长度。
     * 合适的曲柄长度能确保踩踏圆周运动中，膝关节与髋关节的角度在安全且高效的范围内波动。
     * TT车: 建议比标准值短 2.5mm。短曲柄能打开髋关节角度，减少趴姿时大腿撞击腹部的感觉。
     *
     * @param spanHeight 跨高 (mm)
     * @param bikeType   单车类型
     * @return 建议曲柄长度 (mm)，取整至最近的 2.5mm (如 170, 172.5)
     */
    public static int calcCrankLength(int spanHeight, int bikeType) {
        // 采用比例法 (跨高 * 0.204)，并适配市面主流 2.5mm 递进规格
        double raw = spanHeight * 0.204;
        // TT车修正：强制短曲柄
        if (bikeType == BikeType.TT_BIKE.code) {
            raw -= 2.5;
        }
        return (int)(Math.round(raw / 2.5) * 2.5);
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
        // 公路车姿态前倾，偏移量较小；山地车姿态较直立，需要更大支撑面
        int offset;
        if (bikeType == BikeType.CITY_BIKE.code) {
            offset = 40;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            offset = 30;
        } else if (bikeType == BikeType.GRAVEL_BIKE.code) {
            offset = 25;
        } else {
            // Road & TT
            offset = 20;
        }
        return ischialDistance + offset;
    }

    // --- LEVEL 3: 高级修正算法 (Biomechanical Adjustments) ---

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
            k = 0.35;
        } else if (femurRatio < 0.45) {
            k = 0.29;
        } else {
            k = 0.32;
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
        if (calf <= 0 || spanHeight <= 0) return 0;
        double ratio = (double) calf / spanHeight;
        // 小腿占跨高比例 > 0.52 时，强制降低5mm以保护跟腱
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
        // 核心公式：理想 Reach+ = (躯干 + 手臂) / 2.1
        // 针对不同车型微调
        double baseReach = (trunk + arms) / 2.1;
        
        double typeFactor = 1.0;
        if (bikeType == BikeType.TT_BIKE.code) {
            typeFactor = 1.05;
        } else if (bikeType == BikeType.CITY_BIKE.code) {
            typeFactor = 0.85;
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            typeFactor = 0.92;
        } else if (bikeType == BikeType.GRAVEL_BIKE.code) {
            typeFactor = 0.98;
        }
        
        // 柔韧性补偿：得分越高，越能耐受前伸
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
            dropK = 0.060;
        } else if (bikeType == BikeType.CITY_BIKE.code) {
            dropK = -0.030; // 负值表示车把高于座垫
        } else if (bikeType == BikeType.MOUNTAIN_BIKE.code) {
            dropK = 0.020;
        } else if (bikeType == BikeType.GRAVEL_BIKE.code) {
            dropK = 0.035;
        } else {
            // Road
            dropK = 0.045;
        }
        double drop = height * dropK;
        // 灵活性惩罚逻辑 (TT车对灵活性要求更高)
        if (armRaiseAngle > 0 && armRaiseAngle < 155) {
            // 如果是City Bike，本身就高，不需要惩罚
            // 如果是TT/Road，需要大幅减少落差
            if (bikeType != BikeType.CITY_BIKE.code) {
                drop *= 0.7;
            }
        }
        // 柔韧性惩罚：胸椎灵活性得分低于 60 分时，主动减少 15% 的落差（即抬高车头）
        if (thoracicScore != null && thoracicScore < 60) {
            drop *= 0.85;
        }
        return Math.round(drop * 10.0) / 10.0;
    }
}
