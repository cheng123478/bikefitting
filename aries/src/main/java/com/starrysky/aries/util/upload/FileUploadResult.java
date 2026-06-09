package com.starrysky.aries.util.upload;

import lombok.Getter;
import lombok.Setter;

/**
 * FileUploadResponse
 * 文件上传后，响应实体类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Setter
@Getter
public class FileUploadResult {
    private boolean result;
    private String message;
    private String originalName;
    private String name;
    private String url;
}
