package com.starrysky.aries.userbike.repository;

import com.starrysky.aries.entity.UserBikeFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户收藏Repository
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@Repository("userBikeUserBikeFavoriteRepository")
public interface UserBikeFavoriteRepository extends JpaRepository<UserBikeFavorite, Long> {
    
    /**
     * 查询用户是否已收藏某车型
     */
    @Query("SELECT f FROM UserBikeFavorite f WHERE f.user.id = :userId AND f.bikeModel.id = :bikeModelId")
    Optional<UserBikeFavorite> findByUserIdAndBikeModelId(@Param("userId") Long userId, 
                                                           @Param("bikeModelId") Long bikeModelId);
    
    /**
     * 查询用户的收藏列表(分页)
     */
    @Query("SELECT f FROM UserBikeFavorite f WHERE f.user.id = :userId ORDER BY f.createTime DESC")
    Page<UserBikeFavorite> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 统计用户收藏数量
     */
    @Query("SELECT COUNT(f) FROM UserBikeFavorite f WHERE f.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 删除用户的收藏
     */
    void deleteByUserIdAndId(Long userId, Long id);
}
