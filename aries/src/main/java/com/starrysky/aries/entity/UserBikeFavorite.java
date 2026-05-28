package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 用户单车收藏关联表
 *
 * @author adi
 * @version 1.0
 * @since 2026/5/22
 */
@Entity
@Table(name = "user_bike_favorite", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "bike_id"})
})
@Comment("用户单车收藏关联表")
@Setter
@Getter
public class UserBikeFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("用户ID")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Comment("单车ID")
    @Column(name = "bike_id", nullable = false)
    private Long bikeId;

    @Comment("创建时间")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}