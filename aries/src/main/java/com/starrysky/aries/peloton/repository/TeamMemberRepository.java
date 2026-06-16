package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 分页查询某车队的活跃成员（按入队时间升序）
     *
     * @param teamId   车队ID
     * @param status   状态值
     * @param pageable 分页参数
     * @return 成员分页列表
     */
    Page<TeamMember> findByTeamIdAndStatusOrderByJoinDateAsc(Long teamId, Integer status, Pageable pageable);

    /**
     * 按关键词分页搜索车队活跃成员（匹配用户昵称或骑行标签）
     * <p>
     * JOIN FETCH User 表以支持昵称模糊匹配，同时匹配成员骑行标签。
     *
     * @param teamId   车队ID
     * @param status   状态值
     * @param keyword  关键词（匹配昵称或标签）
     * @param pageable 分页参数
     * @return 成员分页列表
     */
    @Query("SELECT DISTINCT m FROM TeamMember m JOIN User u ON m.userId = u.id " +
            "WHERE m.teamId = :teamId AND m.status = :status AND (" +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.tag) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<TeamMember> searchByKeyword(
            @Param("teamId") Long teamId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable);

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

    /**
     * 物理删除某车队所有成员（I6: 解散车队时使用）
     */
    @Modifying
    @Query("DELETE FROM TeamMember m WHERE m.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);
}
