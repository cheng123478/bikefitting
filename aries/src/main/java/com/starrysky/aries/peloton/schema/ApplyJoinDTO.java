package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ApplyJoinDTO
 * 申请加入车队请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class ApplyJoinDTO {

    /** 申请理由（必填，最多200字） */
    @NotBlank(message = "请填写申请理由")
    @Size(max = 200, message = "申请理由最多200字")
    private String reason;
}
