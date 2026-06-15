package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TeamNoticeVO
 * 车队公告响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamNoticeVO {

    /** 公告ID */
    private Long id;

    /** 所属车队ID */
    private Long teamId;

    /** 发布者用户ID */
    private Long authorId;

    /** 发布者昵称 */
    private String authorName;

    /** 发布者头像 */
    private String authorAvatar;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
