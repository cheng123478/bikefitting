package com.starrysky.aries.util;

/**
 * 单车类型
 *
 * @author adi
 * @version 1.0
 * @since 2020/5/27 16:05
 */
public enum BikeType {
    // 公路车(标准竞技/综合)
    //
    ROAD_BIKE(1, "公路车", 1.45, 0.883, 0.445),
    // 山地车(XC/Trail)
    // STR ~1.25(看似低其实是因为Reach长), 伸展系数 0.410 (把立短
    MOUNTAIN_BIKE(2, "山地车", 1.25, 0.880, 0.420),
    // Gravel(长途/碎石)
    // STR ~1.55 (车头高), 伸展系数 0.435 (略微直立)
    GRAVEL_BIKE(3, "全地形公路车", 1.55, 0.881, 0.435),
    // 城市/平把公路 (City/Hybrid)
    // STR极高(舒适),座高系数略低(便于停车),伸展系数最小(直立),
    CITY_BIKE(4, "城市通勤车", 1.60, 0.875, 0.380),
    // 铁三/计时车(Triathlon/TT)
    // STR极低(气动),座高系数略高(因为腿伸得更直),伸展系数极大(趴姿)
    TT_BIKE(5, "铁三/计时车", 1.30, 0.885, 0.475);

    public final int code;
    public final String displayName;
    public final double stackReachRatio;           // 基础Stack/Reach比
    public final double seatHeightCoefficient;      // 座高系数
    public final double reachCoefficient;          // 前伸系数

    BikeType(int code, String displayName, double stackReachRatio, double seatHeightCoefficient, double reachCoefficient) {
        this.code = code;
        this.displayName = displayName;
        this.stackReachRatio = stackReachRatio;
        this.seatHeightCoefficient = seatHeightCoefficient;
        this.reachCoefficient = reachCoefficient;
    }
}
