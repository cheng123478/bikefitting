package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamGarage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * TeamGarageRepository
 * 车队车库数据访问层
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
public interface TeamGarageRepository extends JpaRepository<TeamGarage, Long> {

    /**
     * 查询车队车库中所有正常状态的记录
     *
     * @param teamId 车队ID
     * @param status 状态值
     * @return 车库记录列表
     */
    List<TeamGarage> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, Integer status);

    /**
     * 查询某辆单车在车队车库中的记录（用于唯一性校验）
     *
     * @param teamId 车队ID
     * @param bikeId 单车ID
     * @return 车库记录
     */
    Optional<TeamGarage> findByTeamIdAndBikeId(Long teamId, Long bikeId);

    /**
     * 查询用户在指定车队车库中的所有正常状态记录
     *
     * @param teamId 车队ID
     * @param userId 用户ID
     * @param status 状态值
     * @return 车库记录列表
     */
    List<TeamGarage> findByTeamIdAndUserIdAndStatus(Long teamId, Long userId, Integer status);

    /**
     * 批量查询车队车库记录（按 ID 列表）
     *
     * @param ids 记录ID集合
     * @return 车库记录列表
     */
    List<TeamGarage> findByIdIn(List<Long> ids);

    /**
     * 统计车队车库中正常状态的单车数量
     *
     * @param teamId 车队ID
     * @param status 状态值
     * @return 数量
     */
    long countByTeamIdAndStatus(Long teamId, Integer status);

    /**
     * 将车队车库中所有正常状态记录标记为已移除（解散车队时使用）
     *
     * @param teamId    车队ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     */
    @Modifying
    @Query("UPDATE TeamGarage t SET t.status = :newStatus WHERE t.teamId = :teamId AND t.status = :oldStatus")
    void updateStatusByTeamIdAndStatus(@Param("teamId") Long teamId,
                                       @Param("oldStatus") Integer oldStatus,
                                       @Param("newStatus") Integer newStatus);
}
