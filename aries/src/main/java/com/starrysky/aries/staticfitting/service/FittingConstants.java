package com.starrysky.aries.staticfitting.service;

/**
 * 静态 Fitting 算法常量定义
 * 统一管理生物力学系数，实现逻辑与参数解耦
 *
 * @author adi
 * @version 1.0
 * @since 2025/12/23 15:15
 */
public class FittingConstants {
    // --- 座高系数 (LeMond) ---
    public static final double ROAD_LEMOND_K = 0.883;
    public static final double TT_LEMOND_K = 0.885;
    public static final double MTB_LEMOND_K = 0.880;
    public static final double CITY_LEMOND_K = 0.870;

    // --- 堆高系数 (Stack) ---
    public static final double ROAD_STACK_K = 0.69;
    public static final double TT_STACK_K = 0.62;
    public static final double MTB_STACK_K = 0.70;
    public static final double CITY_STACK_K = 0.75;

    // --- 曲柄比例 ---
    public static final double CRANK_RATIO = 0.204;

    // --- 后移量系数 (Setback) ---
    public static final double SETBACK_K_LONG_FEMUR = 0.35;
    public static final double SETBACK_K_STANDARD = 0.32;
    public static final double SETBACK_K_SHORT_FEMUR = 0.29;

    // --- 前伸量系数 (Reach+) ---
    public static final double REACH_PLUS_BASE_DIVIDER = 2.1;
    public static final double REACH_PLUS_TT_FACTOR = 1.05;
    public static final double REACH_PLUS_ROAD_FACTOR = 1.0;
    public static final double REACH_PLUS_MTB_FACTOR = 0.92;
    public static final double REACH_PLUS_CITY_FACTOR = 0.85;

    // --- 落差系数 (Drop) ---
    public static final double DROP_K_TT = 0.060;
    public static final double DROP_K_ROAD = 0.045;
    public static final double DROP_K_MTB = 0.020;
    public static final double DROP_K_CITY = -0.030; // 负值表示高于座垫

    // --- 人体比例校验阈值 ---
    public static final double SPAN_HEIGHT_RATIO_MIN = 0.43;
    public static final double SPAN_HEIGHT_RATIO_MAX = 0.52;
    public static final double APE_INDEX_MAX = 1.10;
    public static final double APE_INDEX_MIN = 0.90;
    public static final double BMI_OBESITY_THRESHOLD = 30.0;
    public static final double WAIST_HEIGHT_RATIO_RISK_THRESHOLD = 0.50;
    public static final double ABDOMINAL_SEVERE_WAIST_HEIGHT_RATIO = 0.60;
    public static final int ABDOMINAL_REACH_REDUCTION_MILD = 10;
    public static final int ABDOMINAL_REACH_REDUCTION_SEVERE = 20;
    public static final int ABDOMINAL_DROP_REDUCTION_MILD = 8;
    public static final int ABDOMINAL_DROP_REDUCTION_SEVERE = 15;
}
