package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 车型统计表
 * 用于记录整个车型（不分尺寸）的浏览量、收藏量等统计数据
 *
 * @author adi
 * @since 2026/05/25
 */
@Table(name = "bike_model_stats")
@Comment("车型统计数据表")
@Setter
@Getter
@Entity
public class BikeModelStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model", nullable = false, unique = true, length = 200)
    @Comment("车型型号（唯一标识）")
    private String model;

    @Column(name = "view_count")
    @Comment("浏览量")
    private Long viewCount = 0L;

    @Column(name = "favorite_count")
    @Comment("收藏量")
    private Long favoriteCount = 0L;

    @Column(name = "recommend_weight")
    @Comment("推荐权重（用于排序）")
    private Double recommendWeight = 0.0;
}
