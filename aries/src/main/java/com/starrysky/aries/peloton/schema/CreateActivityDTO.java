package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * CreateActivityDTO
 * 发布/编辑车队活动请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Data
public class CreateActivityDTO {

    /** 活动标题（必填，2-50字） */
    @NotBlank(message = "活动标题不能为空")
    @Size(min = 2, max = 50, message = "活动标题需在2-50字之间")
    private String title;

    /** 活动类型（必填）: ride/race/training/social */
    @NotBlank(message = "活动类型不能为空")
    @Pattern(regexp = "ride|race|training|social", message = "活动类型不合法")
    private String activityType;

    /** 活动日期（必填，格式: yyyy-MM-dd） */
    @NotBlank(message = "活动日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String eventDate;

    /** 开始时间（可选，格式: HH:mm） */
    @Pattern(regexp = "\\d{2}:\\d{2}(:\\d{2})?", message = "时间格式应为 HH:mm")
    private String eventTime;

    /** 活动地点（必填） */
    @NotBlank(message = "活动地点不能为空")
    @Size(max = 100, message = "活动地点不能超过100字")
    private String location;

    /** 骑行路线（可选） */
    @Size(max = 200, message = "骑行路线不能超过200字")
    private String route;

    /** 难度等级（可选）: easy/medium/hard */
    @Pattern(regexp = "easy|medium|hard", message = "难度等级不合法")
    private String difficulty;

    /** 预计里程(km)（可选） */
    @Min(value = 0, message = "里程不能为负数")
    private Integer distance;

    /** 人数上限（可选） */
    @Min(value = 1, message = "人数上限至少为1")
    private Integer maxParticipants;

    /** 活动描述（必填，2-2000字） */
    @NotBlank(message = "活动描述不能为空")
    @Size(min = 2, max = 2000, message = "活动描述需在2-2000字之间")
    private String description;

    /** 封面图URL（可选） */
    @Size(max = 500, message = "封面图URL不能超过500字符")
    @Pattern(regexp = "^https?://.*", message = "封面图必须为有效URL")
    private String coverImage;
}
