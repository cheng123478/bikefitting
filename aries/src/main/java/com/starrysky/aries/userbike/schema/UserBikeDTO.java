package com.starrysky.aries.userbike.schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户单车信息DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/10 21:53
 */
@Setter
@Getter
public class UserBikeDTO {
    private Long id;
    private Long userId;

    // --- 1. 基础身份 ---
    private Long bikeModelId; // 如果选择了已有单车型号
    private String brand;
    private String brandCn;
    private String model;
    private String modelYear;
    private String size;
    private String imageUrl;
    private Integer bikeType;
    private String remark;
    private Integer status;

    // --- 2. 车架几何常量 (Frame Geometry) ---
    private Integer stack;
    private Integer reach;
    private Double seatTubeAngle;
    private Double headTubeAngle;
    private Integer bbDrop;
    private Integer wheelBase;
    private Integer standOverHeight;

    // --- 3. 实车当前设定 (Current Setup) ---
    private Integer crankLength;
    private Integer stemLength;
    private Double stemAngle;
    private Integer spacersHeight;
    private Integer handlebarWidth;
    private Integer handlebarDrop;
    private Integer saddleWidth;
    private Integer saddleHeight;
    private Integer saddleSetback;
    private Double saddleTilt;
    private Integer pedalType;
}
