package com.starrysky.aries.dynamicfitting.schema;

import com.starrysky.aries.entity.DynamicFittingSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

/**
 * 动态Fitting快照VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/6 8:46
 */
@Setter
@Getter
public class DynamicFittingSnapshotVO {
    private Long id;
    private Long sessionId;
    private String viewType;
    private String label; // 原 tag
    private String url; // 原 imageUrl
    private BigDecimal metricValue; // 原 frameVal
}
