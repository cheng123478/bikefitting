package com.starrysky.aries.dynamicfitting.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private Long bodyId;
    private Integer status;
    private String deviceInfo;
    private LocalDateTime sessionTime;
    private LocalDateTime createTime;
}
