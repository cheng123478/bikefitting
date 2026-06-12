package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PelotonTeamVO
 * 车队列表/详情响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PelotonTeamVO {

    /** 车队ID */
    private Long id;

    /** 创建者用户ID */
    private Long creatorId;

    /** 车队名称 */
    private String name;

    /** 车队头像 */
    private String avatar;

    /** 车队封面图 */
    private String coverImage;

    /** 所在地 */
    private String location;

    /** 车队简介 */
    private String description;

    /** 成员数量 */
    private Integer memberCount;

    /** 活跃度等级: hot / normal / new */
    private String activityLevel;

    /** 标签列表 */
    private List<String> tags;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
