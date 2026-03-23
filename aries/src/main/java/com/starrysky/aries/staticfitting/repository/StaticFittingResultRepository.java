package com.starrysky.aries.staticfitting.repository;

import com.starrysky.aries.entity.StaticFittingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface StaticFittingResultRepository extends JpaRepository<StaticFittingResult, Long> {
    Optional<StaticFittingResult> findTopByUserBodyIdAndUserBikeIdOrderByFittingTimeDesc(Long userBodyInfoId,
            Long userBikeId);

    @Query("SELECT r.userBikeId FROM StaticFittingResult r " +
            "JOIN UserBody u ON r.userBodyId = u.id " +
            "WHERE u.user.id = :userId")
    Set<Long> findBikeIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT r, b.bikeModel.model " +
            "FROM StaticFittingResult r " +
            "JOIN UserBody u ON r.userBodyId = u.id " +
            "LEFT JOIN UserBike b ON r.userBikeId = b.id " +
            "WHERE u.user.id = :userId " +
            "ORDER BY r.fittingTime DESC")
    Page<Object[]> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);
}
