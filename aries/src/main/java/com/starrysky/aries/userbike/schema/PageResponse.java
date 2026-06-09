package com.starrysky.aries.userbike.schema;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 分页响应DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    private Integer size;
    
    /**
     * 总页数
     */
    private Integer pages;
}
