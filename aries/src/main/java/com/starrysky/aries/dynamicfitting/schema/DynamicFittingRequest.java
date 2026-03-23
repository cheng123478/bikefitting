package com.starrysky.aries.dynamicfitting.schema;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DynamicFittingRequest {
    private Long bikeId;
    private List<ViewDetailsDTO> views;
    private OffsetDateTime sessionTime;

    @Data
    public static class ViewDetailsDTO {
        private String viewType;
        private Map<String, Object> summary;
        private Map<String, Object> triggerMetrics;
        private Map<String, String> snapshots; // tag -> base64
        private Map<String, Object[]> motionData;
    }
}
