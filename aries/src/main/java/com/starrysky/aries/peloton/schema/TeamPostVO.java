package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TeamPostVO
 * 车队动态响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPostVO {

    /** 动态ID */
    private Long id;

    /** 所属车队ID */
    private Long teamId;

    /** 发布者用户ID */
    private Long authorId;

    /** 发布者昵称 */
    private String authorName;

    /** 发布者头像 */
    private String authorAvatar;

    /** 动态文本内容 */
    private String content;

    /** 图片URL列表 */
    private List<String> images;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 评论列表 */
    private List<TeamPostCommentVO> comments;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
