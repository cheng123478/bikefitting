package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * TeamGarageVO
 * 车队车库响应视图对象
 * <p>
 * 聚合车主信息 + 单车几何信息，供前端直接渲染车库卡片。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamGarageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 车库记录ID */
    private Long id;

    /** 关联个人车库单车ID */
    private Long bikeId;

    /** 车主用户ID */
    private Long ownerId;

    /** 车主昵称 */
    private String ownerName;

    /** 车主头像 */
    private String ownerAvatar;

    /** 品牌（英文，如 Trek） */
    private String brand;

    /** 中文品牌（如 崔克） */
    private String brandCn;

    /** 型号（如 Tarmac SL8） */
    private String model;

    /** 车型分类文本（公路车 / 山地车 / Gravel 等） */
    private String type;

    /** 车架尺码（如 52 / M） */
    private String frameSize;

    /** 款型年份 */
    private String modelYear;

    /** 车型图片 */
    private String imageUrl;

    /** 展示色值（前端卡片装饰用） */
    private String color;

    /** 入队时间 */
    private LocalDateTime addedAt;
}
