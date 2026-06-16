package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TeamPostInteraction
 * 车队动态互动实体（映射 team_post_interaction 表）
 * 涵盖点赞(type=0)与评论(type=1)两种交互类型。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Entity
@Table(name = "team_post_interaction",
       indexes = {
           @Index(name = "idx_post_type", columnList = "post_id, type, status"),
           @Index(name = "idx_user", columnList = "user_id")
       })
@Comment("车队动态互动表（点赞/评论）")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamPostInteraction {

    /** 互动类型: 点赞 */
    public static final int TYPE_LIKE = 0;
    /** 互动类型: 评论 */
    public static final int TYPE_COMMENT = 1;

    /** 状态: 正常 */
    public static final int STATUS_ACTIVE = 0;
    /** 状态: 已删除/已取消 */
    public static final int STATUS_DELETED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    @Comment("所属动态")
    private Long postId;

    @Column(name = "user_id", nullable = false)
    @Comment("操作用户")
    private Long userId;

    @Column(nullable = false)
    @Comment("类型: 0点赞 1评论")
    private Integer type;

    @Column(length = 200)
    @Comment("评论内容(type=1时使用)")
    private String content;

    @Column(nullable = false)
    @Comment("状态: 0正常 1已删除/已取消")
    private Integer status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
