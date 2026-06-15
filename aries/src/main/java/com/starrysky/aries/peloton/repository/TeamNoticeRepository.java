package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * TeamNoticeRepository
 * 车队公告数据访问层（映射 team_notice 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
public interface TeamNoticeRepository extends JpaRepository<TeamNotice, Long> {

    /**
     * 查询某车队的正常公告（带分页，用于公告栏展示）
     *
     * @param teamId   车队ID
     * @param status   状态值
     * @param pageable 分页参数
     * @return 公告分页列表
     */
    @Query("SELECT n FROM TeamNotice n WHERE n.teamId = :teamId AND n.status = :status ORDER BY n.createdAt DESC")
    Page<TeamNotice> findActiveNoticesByTeamId(
            @Param("teamId") Long teamId,
            @Param("status") Integer status,
            Pageable pageable);

    /**
     * 查询某条正常状态的公告
     *
     * @param id     公告ID
     * @param status 状态值
     * @return 公告（如存在且状态匹配）
     */
    Optional<TeamNotice> findByIdAndStatus(Long id, Integer status);

    /**
     * 批量将某车队所有正常公告标记为已删除（解散车队时使用）
     *
     * @param teamId    车队ID
     * @param status    当前状态
     * @param newStatus 目标状态
     */
    @Modifying
    @Query("UPDATE TeamNotice n SET n.status = :newStatus WHERE n.teamId = :teamId AND n.status = :status")
    void updateStatusByTeamIdAndStatus(
            @Param("teamId") Long teamId,
            @Param("status") Integer status,
            @Param("newStatus") Integer newStatus);
}
