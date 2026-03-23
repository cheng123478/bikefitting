package com.starrysky.aries.bikemodel.schema;

import lombok.Data;

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
}
