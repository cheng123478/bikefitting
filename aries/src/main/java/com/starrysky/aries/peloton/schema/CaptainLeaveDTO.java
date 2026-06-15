package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CaptainLeaveDTO
 * 队长退出车队（转让队长身份）请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class CaptainLeaveDTO {

    /** 新队长的用户ID（必填） */
    @NotNull(message = "请指定新队长")
    private Long newCaptainUserId;
}
