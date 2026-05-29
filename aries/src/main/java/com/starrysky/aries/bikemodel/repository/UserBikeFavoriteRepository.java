package com.starrysky.aries.bikemodel.repository;

import com.starrysky.aries.entity.UserBikeFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户单车收藏 Repository
 *
 * @author adi
 * @version 1.0
 * @since 2026/5/22
 */
@Repository
public interface UserBikeFavoriteRepository extends JpaRepository<UserBikeFavorite, Long> {

    /**
     * 判断用户是否收藏了某单车
     *
     * @param userId 用户ID
     * @param bikeId 单车ID
     * @return 是否收藏
     */
    boolean existsByUserIdAndBikeId(Long userId, Long bikeId);

    /**
     * 获取用户收藏的单车ID列表
     *
     * @param userId 用户ID
     * @return 单车ID列表
     */
    @Query("SELECT f.bikeId FROM UserBikeFavorite f WHERE f.userId = :userId")
    List<Long> findFavoriteBikeIdsByUserId(@Param("userId") Long userId);

    /**
     * 删除用户对某单车的收藏
     *
     * @param userId 用户ID
     * @param bikeId 单车ID
     */
    void deleteByUserIdAndBikeId(Long userId, Long bikeId);

    /**
     * 根据用户ID查询收藏列表，按创建时间降序排序
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<UserBikeFavorite> findByUserIdOrderByCreateTimeDesc(Long userId);
}