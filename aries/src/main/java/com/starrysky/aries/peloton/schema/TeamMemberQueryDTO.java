package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * TeamMemberQueryDTO
 * 车队成员列表查询参数（分页 + 关键词搜索）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Data
public class TeamMemberQueryDTO {

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 成员列表每页数量 */
    public static final int DEFAULT_SIZE = 20;

    /** 页码（默认1，最小1） */
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = DEFAULT_PAGE;

    /** 每页数量（默认20，范围1-50） */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能超过50")
    private Integer size = DEFAULT_SIZE;

    /** 搜索关键词（模糊匹配成员昵称或骑行标签） */
    private String keyword;
}
