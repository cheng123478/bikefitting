package com.starrysky.aries.bikerecommend.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 推荐缓存数据项
 * 缓存去重后的单车ID、车型名称、价格、预计算的热度分
 *
 * @author adi
 * @since 2026/06/01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BikeRecommendCacheDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 去重后的单车ID（每个车型取最小id）
     */
    private Long bikeId;

    /**
     * 车型名称
     */
    private String model;

    /**
     * 价格
     */
    private Integer price;

    /**
     * 预计算的热度分
     */
    private Double hotScore;
}
