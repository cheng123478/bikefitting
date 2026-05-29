package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * 车型统计表
 * 用于记录整个车型（不分尺寸）的浏览量、收藏量等统计数据
 * 同一型号选择最小 id 的尺寸作为代表进行统计
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
    
    @Column(name = "bike_model_id", nullable = false, unique = true)
    @Comment("车型ID（关联bike_model表的最小id，代表整个车型）")
    private Long bikeModelId;
    
    @Column(name = "view_count")
    @Comment("浏览量")
    private Long viewCount = 0L;
    
    @Column(name = "favorite_count")
    @Comment("收藏量")
    private Long favoriteCount = 0L;
    
    @Column(name = "recommend_weight")
    @Comment("推荐权重（用于排序）")
    private Double recommendWeight = 0.0;
    
    @Column(name = "create_time")
    @Comment("创建时间")
    private java.time.LocalDateTime createTime;
    
    @Column(name = "update_time")
    @Comment("更新时间")
    private java.time.LocalDateTime updateTime;
}
