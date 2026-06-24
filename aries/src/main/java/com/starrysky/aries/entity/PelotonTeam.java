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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @Column(length = 500)
    @Comment("标签，逗号分隔，如: 公路,爬坡,长途")
    private String tags;

    @Column(name = "creator_id")
    @Comment("原始创建者用户ID（创建后不变，用于审计）")
    private Long creatorId;

    @Column(name = "captain_id")
    @Comment("当前队长用户ID（队长转让后此值会更新）")
    private Long captainId;

    @CreationTimestamp
    @Comment("创建时间")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Comment("更新时间")
    private LocalDateTime updateTime;

    // ============================ 惰性缓存 ============================

    /**
     * I5: 按需惰性缓存 tags 解析结果，避免每次 convertToVO 都执行 split 产生临时数组
     */
    @Transient
    private List<String> tagListCache;

    /**
     * 获取标签列表（惰性缓存），避免重复 split
     *
     * @return 标签列表，永不为 null
     */
    public List<String> getTagList() {
        if (tagListCache == null && tags != null && !tags.isEmpty()) {
            tagListCache = Arrays.asList(tags.split(","));
        }
        return tagListCache != null ? tagListCache : Collections.emptyList();
    }
}
