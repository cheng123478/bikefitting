package com.starrysky.aries.bikemodel.repository;

import com.starrysky.aries.entity.BikeModelStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 车型统计数据Repository
 *
 * @author adi
 * @since 2026/05/25
 */
public interface BikeModelStatsRepository extends JpaRepository<BikeModelStats, Long> {
    
    /**
     * 批量查询统计数据
     *
     * @param bikeModelIds 车型ID列表
     * @return 统计数据列表
     */
    List<BikeModelStats> findByBikeModelIdIn(List<Long> bikeModelIds);
    
    /**
     * 根据车型ID查找统计数据
     *
     * @param bikeModelId 车型ID（代表尺寸的最小id）
     * @return 统计数据
     */
    Optional<BikeModelStats> findByBikeModelId(Long bikeModelId);
    
    /**
     * 增加浏览量（原子操作）
     *
     * @param bikeModelId 车型ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE BikeModelStats s SET s.viewCount = s.viewCount + 1 WHERE s.bikeModelId = :bikeModelId")
    int incrementViewCount(@Param("bikeModelId") Long bikeModelId);
    
    /**
     * 增加收藏量（原子操作）
     *
     * @param bikeModelId 车型ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE BikeModelStats s SET s.favoriteCount = s.favoriteCount + 1 WHERE s.bikeModelId = :bikeModelId")
    int incrementFavoriteCount(@Param("bikeModelId") Long bikeModelId);
    
    /**
     * 减少收藏量（原子操作）
     *
     * @param bikeModelId 车型ID
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE BikeModelStats s SET s.favoriteCount = CASE WHEN s.favoriteCount > 0 THEN s.favoriteCount - 1 ELSE 0 END WHERE s.bikeModelId = :bikeModelId")
    int decrementFavoriteCount(@Param("bikeModelId") Long bikeModelId);
}
