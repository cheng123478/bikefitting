package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 动态Fitting会话持久化接口
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@Repository
public interface DynamicFittingSessionRepository extends JpaRepository<DynamicFittingSession, Long> {

    /**
     * 使用 EntityGraph 一次性抓取关联数据，防止 N+1
     * 加入 IsDeleted 条件
     *
     * @param id 会话ID
     * @return 会话数据
     */
    @NotNull
    @EntityGraph(attributePaths = {"userBike", "userBike.bikeModel", "userBody"})
    Optional<DynamicFittingSession> findById(@NotNull Long id);

    /**
     * 使用 EntityGraph 一次性抓取关联数据，防止 N+1
     * 加入 IsDeleted 条件
     *
     * @param userId 用户ID
     * @param isDeleted 是否删除
     * @param pageable 分页参数
     * @return 分页数据
     */
    @EntityGraph(attributePaths = {"userBike", "userBody"})
    Page<DynamicFittingSession> findByUser_IdAndIsDeleted(Long userId, Integer isDeleted, Pageable pageable);

    /**
     * 使用 EntityGraph 一次性抓取关联数据，防止 N+1
     * 加入 IsDeleted 条件
     *
     * @param userId 用户ID
     * @param bikeId 单车ID
     * @param isDeleted 是否删除
     * @param pageable 分页参数
     * @return 分页数据
     */
    @EntityGraph(attributePaths = {"userBike", "userBody"})
    Page<DynamicFittingSession> findByUser_IdAndUserBike_IdAndIsDeleted(Long userId, Long bikeId,
                                                                        Integer isDeleted, Pageable pageable);
}
