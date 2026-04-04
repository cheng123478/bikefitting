package com.starrysky.aries.dynamicfitting.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态Fitting会话VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/5 10:28
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DynamicFittingSessionVO {
    private Long id;
    private Long bikeId;
    private String bikeName;
    private String remark;
    private Long bodyId;
    private Integer status;
    private String deviceInfo;
    private LocalDateTime sessionTime;
    private LocalDateTime createTime;
    private List<String> painLevels;
    private String viewTypes; // 包含的视角，如 "side,front"
    
    // 摘要扩展字段: 用于列表页卡片直接展示，避免额外请求细节接口
    private BigDecimal matchScore;
    private List<String> equipmentSuggestions;
}
