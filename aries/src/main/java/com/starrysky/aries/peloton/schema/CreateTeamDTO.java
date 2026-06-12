package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * CreateTeamDTO
 * 创建车队请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Data
public class CreateTeamDTO {

    /** 车队名称（必填，2-50字） */
    @NotBlank(message = "车队名称不能为空")
    @Size(min = 2, max = 50, message = "车队名称需在2-50字之间")
    private String name;

    /** 所在地 */
    @Size(max = 50, message = "所在地最多50字")
    private String location;

    /** 车队简介 */
    @Size(max = 500, message = "简介最多500字")
    private String description;

    /** 标签列表（最多5个，每个最多10字） */
    @Size(max = 5, message = "标签最多5个")
    private List<@Size(max = 10, message = "单个标签最多10字") String> tags;

    /** 车队头像 URL */
    @Size(max = 500, message = "头像URL过长")
    private String avatar;

    /** 车队封面图 URL */
    @Size(max = 500, message = "封面图URL过长")
    private String coverImage;
}
