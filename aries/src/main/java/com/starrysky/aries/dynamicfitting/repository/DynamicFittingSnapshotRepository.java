package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动态Fitting快照持久化接口
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@Repository
public interface DynamicFittingSnapshotRepository extends JpaRepository<DynamicFittingSnapshot, Long> {
    /**
     * 根据会话ID查询快照
     *
     * @param sessionId 会话ID
     * @return 快照列表
     */
    List<DynamicFittingSnapshot> findBySession_Id(Long sessionId);

    /**
     * 根据会话ID和视角类型查询快照
     *
     * @param sessionId 会话ID
     * @param viewType  视角类型
     * @return 快照列表
     */
    List<DynamicFittingSnapshot> findBySession_IdAndViewType(Long sessionId, String viewType);
}
