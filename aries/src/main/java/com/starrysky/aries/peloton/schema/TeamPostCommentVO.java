package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TeamPostCommentVO
 * 车队动态评论视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPostCommentVO {

    /** 评论ID */
    private Long id;

    /** 评论者用户ID */
    private Long userId;

    /** 评论者昵称 */
    private String userName;

    /** 评论者头像 */
    private String userAvatar;

    /** 评论内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
