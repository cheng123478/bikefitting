package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * PelotonTeam
 * 车队实体
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Entity
@Table(name = "team")
@Comment("车队表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PelotonTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;

    @Column(length = 50, nullable = false)
    @Comment("车队名称")
    private String name;

    @Column(length = 500)
    @Comment("车队头像")
    private String avatar;

    @Column(length = 500)
    @Comment("车队封面图")
    private String coverImage;

    @Column(length = 50)
    @Comment("所在地")
    private String location;

    @Column(length = 500)
    @Comment("车队简介")
    private String description;

    @Comment("成员数量（冗余字段）")
    private Integer memberCount;

    @Column(length = 20)
    @Comment("活跃度等级: hot-热门, normal-普通, new-新兴")
    private String activityLevel;

    @Column(length = 500)
    @Comment("标签，逗号分隔，如: 公路,爬坡,长途")
    private String tags;

    @Column(name = "creator_id")
    @Comment("当前队长用户ID（注意：队长转让后此值会变更，非原始创建者）")
    private Long creatorId;

    @CreationTimestamp
    @Comment("创建时间")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Comment("更新时间")
    private LocalDateTime updateTime;
}
