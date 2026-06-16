package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * CreateCommentDTO
 * 发布评论请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class CreateCommentDTO {

    /** 评论内容（必填，1-200字） */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 200, message = "评论内容不能超过200字")
    private String content;
}
