package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query(
            value = """
                    SELECT s
                    FROM DynamicFittingSession s
                    LEFT JOIN FETCH s.userBike ub
                    LEFT JOIN FETCH ub.bikeModel
                    LEFT JOIN FETCH s.userBody
                    WHERE s.user.id = :userId AND s.isDeleted = :isDeleted
                    ORDER BY s.createTime DESC
                    """,
            countQuery = """
                    SELECT COUNT(s.id)
                    FROM DynamicFittingSession s
                    WHERE s.user.id = :userId AND s.isDeleted = :isDeleted
                    """
    )
    Page<DynamicFittingSession> findByUserIdAndIsDeletedWithDetails(@Param("userId") Long userId,
                                                                    @Param("isDeleted") Integer isDeleted,
                                                                    Pageable pageable);

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
    @Query(
            value = """
                    SELECT s
                    FROM DynamicFittingSession s
                    LEFT JOIN FETCH s.userBike ub
                    LEFT JOIN FETCH ub.bikeModel
                    LEFT JOIN FETCH s.userBody
                    WHERE s.user.id = :userId AND s.userBike.id = :bikeId AND s.isDeleted = :isDeleted
                    ORDER BY s.createTime DESC
                    """,
            countQuery = """
                    SELECT COUNT(s.id)
                    FROM DynamicFittingSession s
                    WHERE s.user.id = :userId AND s.userBike.id = :bikeId AND s.isDeleted = :isDeleted
                    """
    )
    Page<DynamicFittingSession> findByUserIdAndBikeIdAndIsDeletedWithDetails(@Param("userId") Long userId,
                                                                              @Param("bikeId") Long bikeId,
                                                                              @Param("isDeleted") Integer isDeleted,
                                                                              Pageable pageable);
}
