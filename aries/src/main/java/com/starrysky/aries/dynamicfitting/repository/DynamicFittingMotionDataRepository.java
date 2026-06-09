package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingMotionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 动态FittingMotion持久化接口
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@Repository
public interface DynamicFittingMotionDataRepository extends JpaRepository<DynamicFittingMotionData, Long> {
    /**
     * 根据会话ID和视角类型查询
     *
     * @param sessionId 会话ID
     * @param viewType  视角类型
     * @return Motion数据
     */
    Optional<DynamicFittingMotionData> findBySession_IdAndViewType(Long sessionId, String viewType);

    /**
     * 根据会话ID统计数量
     *
     * @param sessionId 会话ID
     * @return 数量
     */
    long countBySession_Id(Long sessionId);
}
