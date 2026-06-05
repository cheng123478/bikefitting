package com.starrysky.aries.userbike.schema;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏响应VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteVO {
    /**
     * 收藏记录ID
     */
    private Long id;
    
    /**
     * 车型ID
     */
    private Long bikeModelId;
    
    /**
     * 车型名称
     */
    private String bikeModelName;
    
    /**
     * 品牌英文
     */
    private String bikeModelBrand;
    
    /**
     * 品牌中文
     */
    private String bikeModelBrandCn;
    
    /**
     * 车型图片
     */
    private String bikeModelImg;
    
    /**
     * 车型类型
     */
    private Integer bikeModelType;
    
    /**
     * 车型描述
     */
    private String bikeModelDesc;
    
    /**
     * 价格
     */
    private Integer bikeModelPrice;
    
    /**
     * 收藏时间
     */
    private LocalDateTime createTime;
}
