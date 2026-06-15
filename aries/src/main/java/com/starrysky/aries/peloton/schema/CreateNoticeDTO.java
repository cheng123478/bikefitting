package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * CreateNoticeDTO
 * 发布车队公告请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class CreateNoticeDTO {

    /** 公告标题（必填，2-50字） */
    @NotBlank(message = "公告标题不能为空")
    @Size(min = 2, max = 50, message = "公告标题需在2-50字之间")
    private String title;

    /** 公告内容（必填，2-2000字） */
    @NotBlank(message = "公告内容不能为空")
    @Size(min = 2, max = 2000, message = "公告内容需在2-2000字之间")
    private String content;
}
