package com.starrysky.aries.dynamicfitting.schema;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 动态Fitting请求
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/7 21:23
 */
@Data
public class DynamicFittingRequest {
    private Long bikeId;
    private List<ViewDetailsDTO> views;
    private OffsetDateTime sessionTime;
    private List<String> painLevels;

    @Data
    public static class ViewDetailsDTO {
        private String viewType;
        private Map<String, Object> summary;
        private Map<String, Object> triggerMetrics;
        private Map<String, String> snapshots; // tag -> base64
        private List<Map<String, Object>> frameData;
    }
}
