package com.starrysky.aries.userbike.schema;

import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户单车信息VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/10 22:18
 */
@Setter
@Getter
public class UserBikeVO {
    private Long id;
    private Long userId;
    private LocalDateTime createTime;

    // --- 基础信息 ---
    private String remark;
    private Integer status;

    // --- 嵌套子对象 ---
    private BikeModelVO bikeModelVO;
    private UserBikeSetupVO currentSetupVO;

    // 为了向前兼容或方便前端直接取值，保留一些常用属性从内部对象平级映射
    // （前端也可以改用 vo.getBikeModel().getBrand() 等）
    private String brand;
    private String brandCn;
    private String model;
    private String modelYear;
    private String imageUrl;
    private Integer bikeType;

    // --- Fittor 生物力学分析扩展 ---

    /**
     * 实车 STR (Stack-to-Reach Ratio)
     * 算法：frameStack / frameReach
     * 意义：衡量车架几何的进攻性指标。
     */
    private Double frameStr;

    /**
     * 当前实车有效堆高 Stack+ (mm)
     * 算法预告：frameStack + 垫圈高度 + 把立垂直分量
     * 意义：衡量车头真正的垂直高度。
     */
    private Double effectiveStackPlus;

    /**
     * 当前实车有效前伸量 Reach+ (mm)
     * 算法预告：frameReach + 把立水平分量
     * 意义：衡量座舱真正的拉伸感。
     */
    private Double effectiveReachPlus;
    private boolean hasReport;
}
