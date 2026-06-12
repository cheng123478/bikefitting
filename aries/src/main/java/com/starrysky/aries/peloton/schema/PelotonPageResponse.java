package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PelotonPageResponse
 * 破风社模块分页响应DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PelotonPageResponse<T> {

    /** 数据列表 */
    private List<T> records;

    /** 总数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页数量 */
    private Integer size;

    /** 总页数 */
    private Integer pages;
}
