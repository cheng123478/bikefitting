package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 标准单车信息，主要是几何数据
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/12 10:38
 */
@Entity
@Table(name = "bike_model")
@Comment("标准单车信息,主要是几何数据")
@Setter
@Getter
public class BikeModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // --- 1. 身份识别 ---
    @Comment("品牌 (如: Giant,Trek)")
    private String brand;
    @Comment("中文品牌 (如: 捷安特,崔克)")
    private String brandCn;
    @Comment("系列/型号 (如: Tarmac SL8)")
    private String model;
    @Comment("款型年份 (如: 2024)")
    private String modelYear;
    @Comment("官方尺码 (如: 52, 54, S, M)")
    private String size;
    @Comment("车型分类 (1:公路, 2:山地, 3:Gravel, 4:城市, 5:TT)")
    private Integer bikeType;
    // --- 2. 核心几何数据 (DNA - 毫米/度) ---
    @Comment("标准堆高 Stack (mm)")
    private Integer stack;
    @Comment("标准前伸量 Reach (mm)")
    private Integer reach;
    @Comment("官方立管角度 STA (度)")
    private Double seatTubeAngle;
    @Comment("官方头管角度 HTA (度)")
    private Double headTubeAngle;
    @Comment("五通下沉量")
    private Integer bbDrop;
    @Comment("轮距")
    private Integer wheelBase;
    @Comment("跨高")
    private Integer standOverHeight;
    // --- 3. 辅助信息 ---
    @Comment("车型图片")
    private String imageUrl;
    @Column(columnDefinition = "TEXT COMMENT '车型描述'")
    private String description;
    @Comment("价格")
    private Integer price;
    @Comment("数据来源URL (爬虫抓取的原始页面)")
    private String sourceUrl;
    @Comment("数据来源类型 (1:官方同步, 2:用户手动, 3:AI识别)")
    private Integer sourceType;
    @Comment("是否已审校 (1:待定, 2:官方数据确认)")
    private Integer status;
}
