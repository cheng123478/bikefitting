package com.starrysky.aries.bikemodel.schema;

import lombok.Data;

/**
 * 单车模型视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:10
 */
@Data
public class BikeModelVO {
    private Long id;
    private String brand;
    private String brandCn;
    private String model;
    private String modelYear;
    private String size;
    private Integer bikeType;

    // Geometry
    private Integer stack;
    private Integer reach;
    private Double seatTubeAngle;
    private Double headTubeAngle;
    private Integer bbDrop;
    private Integer wheelBase;
    private Integer standOverHeight;

    // Extra
    private String imageUrl;
    private String description;
    private Integer price;

    // 前端字段别名（用于兼容前端字段名）
    public String getImg() {
        return imageUrl;
    }

    public String getYear() {
        return getModelYear();
    }

    public String getDesc() {
        return description;
    }
}
