package com.starrysky.aries.bikemodel.schema;

import lombok.Data;

/**
 * 单车模型搜索视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:10
 */
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
