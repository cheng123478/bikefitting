package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 原始姿态数据表 - 大字段分离
 *
 * @author antigravity
 * @version 1.0
 * @since 2026/03/03
 */
@Entity
@Table(name = "fit_dynamic_motion_data", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_view", columnNames = { "session_id", "view_type" })
})
@Comment("动态采集原始轨迹序列")
@Getter
@Setter
public class DynamicFittingMotionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联会话")
    private DynamicFittingSession session;

    @Comment("视角: side/front/back")
    @Column(name = "view_type", length = 20, nullable = false)
    private String viewType;

    @Comment("核心时序数组(膝角、坐标等)")
    @Column(columnDefinition = "LONGTEXT")
    private String dataJson;

    @Comment("触发抓拍时的极大/极小值索引")
    @Column(columnDefinition = "JSON")
    private String triggerMetricsJson;

    @Comment("总采集帧数")
    private Integer frameCount;
}
