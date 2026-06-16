package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * TeamActivityRepository
 * 车队活动数据访问层
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
public interface TeamActivityRepository extends JpaRepository<TeamActivity, Long> {

    /**
     * 查询某车队的正常活动（分页，按活动日期倒序）
     *
     * @param teamId   车队ID
     * @param status   状态值
     * @param pageable 分页参数
     * @return 活动分页列表
     */
    @Query("SELECT a FROM TeamActivity a WHERE a.teamId = :teamId AND a.status = :status ORDER BY a.eventDate DESC, a.createdAt DESC")
    Page<TeamActivity> findActiveActivitiesByTeamId(
            @Param("teamId") Long teamId,
            @Param("status") Integer status,
            Pageable pageable);

    /**
     * 查询某条正常状态的活动
     *
     * @param id     活动ID
     * @param status 状态值
     * @return 活动（如存在且状态匹配）
     */
    Optional<TeamActivity> findByIdAndStatus(Long id, Integer status);

    /**
     * 批量将某车队所有正常活动标记为已删除（解散车队时使用）
     *
     * @param teamId    车队ID
     * @param status    当前状态
     * @param newStatus 目标状态
     */
    @Modifying
    @Query("UPDATE TeamActivity a SET a.status = :newStatus WHERE a.teamId = :teamId AND a.status = :status")
    void updateStatusByTeamIdAndStatus(
            @Param("teamId") Long teamId,
            @Param("status") Integer status,
            @Param("newStatus") Integer newStatus);
}
