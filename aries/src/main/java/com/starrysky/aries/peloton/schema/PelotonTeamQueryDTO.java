package com.starrysky.aries.peloton.schema;

import lombok.Data;

/**
 * PelotonTeamQueryDTO
 * 车队列表查询参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Data
public class PelotonTeamQueryDTO {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 9;

    /** 页码（默认1） */
    private Integer page = DEFAULT_PAGE;

    /** 每页数量（默认9） */
    private Integer size = DEFAULT_SIZE;

    /** 搜索关键词（匹配名称、所在地、简介、标签） */
    private String keyword;

    /**
     * 分类筛选:
     * hot  - 热门车队
     * new  - 新兴车队
     * nearby - 附近车队（预留）
     * mine - 我加入的车队（预留）
     */
    private String category;
}
