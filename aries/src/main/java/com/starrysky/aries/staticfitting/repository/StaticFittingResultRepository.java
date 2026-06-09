package com.starrysky.aries.staticfitting.repository;

import com.starrysky.aries.entity.StaticFittingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

/**
 * 静态Fitting结果仓库
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/5 14:13
 */
public interface StaticFittingResultRepository extends JpaRepository<StaticFittingResult, Long> {

    /**
     * 根据用户身体信息ID和用户单车ID查询最新的一条静态Fitting结果
     *
     * @param userBodyInfoId 用户身体信息ID
     * @param userBikeId     用户单车ID
     * @return 静态Fitting结果
     */
    Optional<StaticFittingResult> findTopByUserBodyIdAndUserBikeIdOrderByFittingTimeDesc(Long userBodyInfoId,
                                                                                         Long userBikeId);

    @Query("SELECT r.userBikeId FROM StaticFittingResult r " +
            "JOIN UserBody u ON r.userBodyId = u.id " +
            "WHERE u.user.id = :userId")
    Set<Long> findBikeIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的静态Fitting历史
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 静态Fitting历史
     */
    @Query("SELECT r, b.bikeModel.model " +
            "FROM StaticFittingResult r " +
            "JOIN UserBody u ON r.userBodyId = u.id " +
            "LEFT JOIN UserBike b ON r.userBikeId = b.id " +
            "WHERE u.user.id = :userId " +
            "ORDER BY r.fittingTime DESC")
    Page<Object[]> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户ID统计静态Fitting报告数量
     *
     * @param userId 用户ID
     * @return 报告数量
     */
    @Query("SELECT COUNT(DISTINCT r.id) FROM StaticFittingResult r " +
            "JOIN UserBody u ON r.userBodyId = u.id " +
            "WHERE u.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
