package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

/**
 * 证据快照图片表 - 论据存证
 *
 * @author antigravity
 * @version 1.0
 * @since 2026/03/03
 */
@Entity
@Table(name = "fit_dynamic_snapshot")
@Comment("动态采集关键帧证据")
@Getter
@Setter
public class DynamicFittingSnapshot {

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

    @Comment("标签: bdc/tdc/max_sway")
    @Column(length = 50, nullable = false)
    private String tag;

    @Comment("OSS 或文件存储路径")
    @Column(name = "image_url", length = 512, nullable = false)
    private String imageUrl;

    @Comment("截图瞬间的关键指标数值")
    private BigDecimal frameVal;
}
