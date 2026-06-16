package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * CreatePostDTO
 * 发布车队动态请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class CreatePostDTO {

    /** 动态文本内容（可选，最多500字；图文至少有一项） */
    @Size(max = 500, message = "动态内容不能超过500字")
    private String content;

    /** 图片URL列表（可选，最多9张） */
    @Size(max = 9, message = "图片最多上传9张")
    private List<String> images;
}
