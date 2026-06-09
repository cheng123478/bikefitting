package com.starrysky.aries.bikemodel.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {
    /**
     * 分类ID
     */
    private String id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 图标类名
     */
    private String icon;
}
