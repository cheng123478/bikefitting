package com.starrysky.aries.bikemodel.schema;

import lombok.Data;

@Data
public class BikeModelSearchVO {
    private String brand;
    private String brandCn;
    private String model;

    public BikeModelSearchVO(String brand, String brandCn, String model) {
        this.brand = brand;
        this.brandCn = brandCn;
        this.model = model;
    }
}
