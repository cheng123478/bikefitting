package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UpdateMemberTagDTO
 * 修改成员骑行标签请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class UpdateMemberTagDTO {

    /** 骑行标签（必填，最多20字） */
    @NotBlank(message = "标签不能为空")
    @Size(max = 20, message = "标签最多20字")
    private String tag;
}
