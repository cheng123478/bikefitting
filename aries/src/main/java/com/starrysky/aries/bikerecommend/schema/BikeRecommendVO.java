package com.starrysky.aries.bikerecommend.schema;

import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单车智能推荐响应VO
 * 扩展BikeModelVO,增加评分相关字段
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BikeRecommendVO extends BikeModelVO {
    /**
     * 综合评分(0-100)
     */
    private Double totalScore;
    
    /**
     * 场景匹配分(0-100)
     */
    private Double sceneScore;
    
    /**
     * 几何匹配分(0-100)
     */
    private Double geometryScore;
    
    /**
     * 热度分(0-100+)
     */
    private Double hotScore;
    
    /**
     * 价格匹配分(0-100),未填预算时为null
     */
    private Double priceScore;
    
    /**
     * 最佳尺码的sizeScore(0-100)
     */
    private Integer bestSizeScore;
    
    /**
     * 推荐尺码
     */
    private String recommendedSize;
}
