package com.starrysky.aries.bikemodel.repository;

import com.starrysky.aries.entity.BikeModelStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 车型统计 Repository
 *
 * @author adi
 * @since 2026/05/25
 */
@Repository
public interface BikeModelStatsRepository extends JpaRepository<BikeModelStats, Long> {

    /**
     * 根据车型名称查询统计信息
     *
     * @param model 车型名称
     * @return 统计信息
     */
    Optional<BikeModelStats> findByModel(String model);

    /**
     * 根据车型名称递增收藏量
     *
     * @param model 车型名称
     */
    @Modifying
    @Query("UPDATE BikeModelStats b SET b.favoriteCount = b.favoriteCount + 1 WHERE b.model = :model")
    void incrementFavoriteCountByModel(@Param("model") String model);

    /**
     * 根据车型名称递减收藏量
     *
     * @param model 车型名称
     */
    @Modifying
    @Query("UPDATE BikeModelStats b SET b.favoriteCount = b.favoriteCount - 1 WHERE b.model = :model AND b.favoriteCount > 0")
    void decrementFavoriteCountByModel(@Param("model") String model);

    /**
     * 批量递增收藏量
     *
     * @param models 车型名称列表
     */
    @Modifying
    @Query("UPDATE BikeModelStats b SET b.favoriteCount = b.favoriteCount + 1 WHERE b.model IN :models")
    void incrementFavoriteCountByModels(@Param("models") java.util.List<String> models);

    /**
     * 批量递减收藏量
     *
     * @param models 车型名称列表
     */
    @Modifying
    @Query("UPDATE BikeModelStats b SET b.favoriteCount = b.favoriteCount - 1 WHERE b.model IN :models AND b.favoriteCount > 0")
    void decrementFavoriteCountByModels(@Param("models") java.util.List<String> models);
}
