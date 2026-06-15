package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.PelotonTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PelotonTeamRepository
 * 车队数据访问层
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
public interface PelotonTeamRepository extends JpaRepository<PelotonTeam, Long> {

    /**
     * 按活跃度等级查询车队列表（分页）
     *
     * @param activityLevel 活跃度等级
     * @param pageable      分页参数
     * @return 车队分页列表
     */
    Page<PelotonTeam> findByActivityLevel(String activityLevel, Pageable pageable);

    /**
     * 按关键词模糊搜索车队（匹配名称、所在地、简介、标签）
     *
     * @param keyword  关键词
     * @param pageable 分页参数
     * @return 车队分页列表
     */
    @Query("SELECT t FROM PelotonTeam t WHERE " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PelotonTeam> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 按活跃度等级 + 关键词组合查询
     *
     * @param activityLevel 活跃度等级
     * @param keyword       关键词
     * @param pageable      分页参数
     * @return 车队分页列表
     */
    @Query("SELECT t FROM PelotonTeam t WHERE t.activityLevel = :activityLevel AND (" +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<PelotonTeam> searchByActivityLevelAndKeyword(
            @Param("activityLevel") String activityLevel,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 查询所有车队（按创建时间倒序，分页）
     *
     * @param pageable 分页参数
     * @return 车队分页列表
     */
    Page<PelotonTeam> findAllByOrderByCreateTimeDesc(Pageable pageable);

    /**
     * 根据创建者ID查询其创建的车队
     *
     * @param creatorId 创建者用户ID
     * @return 车队列表
     */
    List<PelotonTeam> findByCreatorId(Long creatorId);

    /**
     * 根据ID列表批量查询车队
     *
     * @param ids 车队ID集合
     * @return 车队列表
     */
    List<PelotonTeam> findByIdIn(List<Long> ids);

    /**
     * 判断车队名称是否已存在
     *
     * @param name 车队名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 查询用户关联的所有车队（创建的 + 加入的，自动去重）
     * <p>
     * 注意：子查询中 {@code m.status = 0} 对应 {@code TeamMember.STATUS_ACTIVE}，
     * 仅统计活跃成员所归属的车队。
     *
     * @param userId 用户ID
     * @return 车队列表
     */
    @Query("SELECT t FROM PelotonTeam t WHERE t.creatorId = :userId " +
            "OR t.id IN (SELECT m.teamId FROM TeamMember m WHERE m.userId = :userId AND m.status = 0)")
    List<PelotonTeam> findTeamsByUserId(@Param("userId") Long userId);

    /**
     * 原子递增车队成员数（避免并发读写覆盖）
     *
     * @param teamId 车队ID
     */
    @Modifying
    @Query("UPDATE PelotonTeam t SET t.memberCount = t.memberCount + 1 WHERE t.id = :teamId")
    void incrementMemberCount(@Param("teamId") Long teamId);

    /**
     * 原子递减车队成员数（不低于0，避免并发读写覆盖）
     *
     * @param teamId 车队ID
     */
    @Modifying
    @Query("UPDATE PelotonTeam t SET t.memberCount = GREATEST(t.memberCount - 1, 0) WHERE t.id = :teamId")
    void decrementMemberCount(@Param("teamId") Long teamId);
}
