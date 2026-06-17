package com.starrysky.aries.notification.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UnreadCountVO
 * 未读消息数量响应视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

    /** 全部未读数 */
    private long total;

    /** 车队动态未读数 */
    private long team;

    /** 互动消息未读数 */
    private long interact;

    /** 系统通知未读数 */
    private long system;
}
