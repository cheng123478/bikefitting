package com.starrysky.aries.bikerecommend.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跨高缓存数据项
 * 缓存每个车型每个尺码的 stand_over_height 值，用于在内存中计算几何匹配分，
 * 替代原有的 SQL 自 JOIN 查询。
 *
 * @author adi
 * @since 2026/06/01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BikeStandOverCacheItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 尺码标签，如 "S", "M", "54"
     */
    private String size;

    /**
     * 立管跨高 stand_over_height (mm)
     */
    private Integer standOverHeight;
}
