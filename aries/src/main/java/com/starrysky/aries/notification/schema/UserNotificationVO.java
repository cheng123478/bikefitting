package com.starrysky.aries.notification.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserNotificationVO
 * 用户通知响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationVO {

    /** 通知ID */
    private Long id;

    /** 消息类型 */
    private String type;

    /** 消息分类 */
    private String category;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联车队ID */
    private Long teamId;

    /** 关联车队名称（通过 teamId JOIN 获取） */
    private String teamName;

    /** 是否已读 */
    private Boolean read;

    /** 跳转目标ID（如 activity_id / post_id / notice_id） */
    private Long linkId;

    /** 链接类型（如 team / lab），供前端根据类型构建路由 */
    private String linkType;

    /** 跳转路由（完整路径，兼容旧版） */
    private String link;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
