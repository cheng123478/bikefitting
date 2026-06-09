package com.starrysky.aries.userbody.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 生物力学计算引擎
 * 封装所有人体比例及评分公式
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 16:00
 */
public class BiometricsCalculator {
    /**
     * 计算猿臂比 (Ape Index)
     * 公式：(双侧手臂总长 + 肩宽) / 身高
     *
     * @param height   身高 (cm)
     * @param shoulder 肩宽 (cm)
     * @param longArm  双侧手臂长度 (cm)
     * @param shortArm 双侧手臂长度 (cm)
     * @return 猿臂比
     */
    public static double calculateApeIndex(int height, int shoulder, int longArm, int shortArm) {
        double armSpan = (longArm + shortArm) * 2.0 + shoulder;
        return round(armSpan / height, 2);
    }

    /**
     * 计算股骨比 (Femur/Inseam Ratio)
     * 公式：大腿长 / 跨高
     *
     * @param thigh 大腿长 (cm)
     * @param inseam 跨高 (cm)
     * @return 股骨比
     */
    public static double calculateFemurRatio(int thigh, int inseam) {
        if (inseam == 0) return 0.0;
        return round((double) thigh / inseam, 2);
    }

    /**
     * 计算躯干占比
     * 公式：胸椎高度 - 肩宽 / 身高
     *
     * @param height       身高 (cm)
     * @param breastHeight 胸椎高度 (cm)
     * @param spanHeight   肩宽 (cm)
     * @return 躯干占比
     */
    public static double calculateTrunkRatio(int height, int breastHeight, int spanHeight) {
        int trunkLength = breastHeight - spanHeight;
        return round((double) trunkLength / height, 2);
    }

    /**
     * 柔韧性综合评分系统 (0-100)
     * 权重：胸椎旋转 60%, 举臂角度 40%
     * 惩罚：每级痛感扣除 10 分
     *
     * @param thoracicRotation 胸椎旋转角度 (0-90)
     * @param armRaise         举臂角度 (0-180)
     * @param painLevel        胸椎痛感等级 (0-5)
     * @return 柔韧性综合评分
     */
    public static int calculateFlexibilityScore(double thoracicRotation, double armRaise, int painLevel) {
        double score = (thoracicRotation / 90.0 * 60.0) + (armRaise / 180.0 * 40.0);
        int finalScore = (int) Math.round(score - (painLevel * 10));
        return Math.max(0, Math.min(100, finalScore));
    }

    /**
     * 四舍五入
     *
     * @param value  待处理的数字
     * @param places 保留的小数位数
     * @return 处理后的数字
     */
    private static double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}
