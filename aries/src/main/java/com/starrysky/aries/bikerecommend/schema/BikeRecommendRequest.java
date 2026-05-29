package com.starrysky.aries.bikerecommend.schema;

import lombok.Data;

import java.util.List;

/**
 * 单车智能推荐请求DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/28
 */
@Data
public class BikeRecommendRequest {
    /**
     * 身高(mm),必填
     */
    private Integer height;
    
    /**
     * 跨高(mm),可选。如未填写则使用身高*0.45计算
     */
    private Integer inseam;
    
    /**
     * 骑行场景ID列表,必填
     * 1:日常通勤代步, 2:运动健身骑行, 3:山地越野骑行, 4:长途户外旅行, 5:速度竞技竞速
     */
    private List<Integer> scenes;
    
    /**
     * 最低价格,可选
     */
    private Integer minPrice;
    
    /**
     * 最高价格,可选
     */
    private Integer maxPrice;
}
