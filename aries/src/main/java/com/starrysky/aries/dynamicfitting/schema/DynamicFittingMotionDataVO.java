package com.starrysky.aries.dynamicfitting.schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 动态Fitting轨迹数据VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/7 21:23
 */
@Setter
@Getter
public class DynamicFittingMotionDataVO {
    private Long id;
    private String viewType;
    private String dataJson; // 原始轨迹数组
    private String triggerMetricsJson;
    private Integer frameCount;
}
