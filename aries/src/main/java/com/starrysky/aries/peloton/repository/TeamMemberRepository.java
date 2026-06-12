package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * TeamMemberRepository
 * 车队成员数据访问层（映射 team_member 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /**
     * 查询某车队的活跃成员（按入队时间升序）
     */
    List<TeamMember> findByTeamIdAndStatusOrderByJoinDateAsc(Long teamId, Integer status);

    /**
     * 查询某用户在某车队的活跃成员记录
     */
    Optional<TeamMember> findByTeamIdAndUserIdAndStatus(Long teamId, Long userId, Integer status);

    /**
     * 查询某用户在任何车队的活跃成员记录（用于"我的车队"查询）
     */
    List<TeamMember> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * 统计某车队的活跃成员数量
     */
    long countByTeamIdAndStatus(Long teamId, Integer status);

    /**
     * 批量将某车队所有活跃成员标记为已退出（解散车队时使用）
     */
    @Modifying
    @Query("UPDATE TeamMember m SET m.status = :newStatus WHERE m.teamId = :teamId AND m.status = :status")
    void updateStatusByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") Integer status, @Param("newStatus") Integer newStatus);
}
