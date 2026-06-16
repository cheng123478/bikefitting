package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PelotonTeamMemberVO
 * 车队成员响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PelotonTeamMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成员记录ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户昵称 */
    private String name;

    /** 用户头像 */
    private String avatar;

    /** 角色: 队长, 管理员, 成员 */
    private String role;

    /** 成员标签 */
    private String tag;

    /** 申请理由 */
    private String reason;

    /** 加入/申请时间 */
    private LocalDateTime joinDate;
}
