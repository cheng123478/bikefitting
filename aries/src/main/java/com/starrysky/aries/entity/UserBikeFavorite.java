package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户收藏的单车车型
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@Entity
@Table(name = "user_bike_favorite")
@Comment("用户收藏的单车车型表")
@Setter
@Getter
public class UserBikeFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;
    
    @CreationTimestamp
    @Comment("创建时间")
    private LocalDateTime createTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联的单车车型")
    private BikeModel bikeModel;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联的用户")
    private User user;
}
