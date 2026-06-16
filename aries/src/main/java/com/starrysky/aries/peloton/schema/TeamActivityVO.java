package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TeamActivityVO
 * 车队活动响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamActivityVO {

    /** 活动ID */
    private Long id;

    /** 所属车队ID */
    private Long teamId;

    /** 发布者用户ID */
    private Long authorId;

    /** 发布者昵称 */
    private String authorName;

    /** 发布者头像 */
    private String authorAvatar;

    /** 活动标题 */
    private String title;

    /** 活动类型: ride/race/training/social */
    private String activityType;

    /** 活动日期 */
    private LocalDate eventDate;

    /** 开始时间 */
    private LocalTime eventTime;

    /** 活动地点 */
    private String location;

    /** 骑行路线 */
    private String route;

    /** 难度等级: easy/medium/hard */
    private String difficulty;

    /** 预计里程(km) */
    private Integer distance;

    /** 人数上限 */
    private Integer maxParticipants;

    /** 活动描述 */
    private String description;

    /** 封面图URL */
    private String coverImage;

    /** 报名人数 */
    private Integer participantCount;

    /** 状态: 0正常 1已取消 2已删除 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
