package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamJoinApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * TeamJoinApplicationRepository
 * 车队加入申请数据访问层
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
public interface TeamJoinApplicationRepository extends JpaRepository<TeamJoinApplication, Long> {

    /**
     * 查询某车队的待审核申请列表（按申请时间倒序）
     *
     * @param teamId 车队ID
     * @param status 状态值
     * @return 申请列表
     */
    List<TeamJoinApplication> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, Integer status);

    /**
     * 查询某用户对某车队的待审核申请
     *
     * @param teamId      车队ID
     * @param applicantId 申请人ID
     * @param status      状态值
     * @return 申请记录
     */
    Optional<TeamJoinApplication> findByTeamIdAndApplicantIdAndStatus(Long teamId, Long applicantId, Integer status);

    /**
     * 删除某车队的所有申请记录（解散车队时使用）
     *
     * @param teamId 车队ID
     */
    void deleteByTeamId(Long teamId);
}
