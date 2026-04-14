package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

/**
 * 动态Fitting结果持久化接口
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
public interface DynamicFittingResultRepository extends JpaRepository<DynamicFittingResult, Long> {

    /**
     * 根据用户ID查询所有单车ID
     *
     * @param userId 用户ID
     * @return 单车ID集合
     */
    @Query("SELECT s.userBike.id FROM DynamicFittingSession s JOIN DynamicFittingResult r ON s.id = r.session.id WHERE s.user.id = :userId")
    Set<Long> findBikeIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据会话ID查询结果
     *
     * @param sessionId 会话ID
     * @return Fitting结果
     */
    Optional<DynamicFittingResult> findBySession_Id(Long sessionId);
}
