package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.PelotonTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * PelotonTeamMemberRepository
 * 车队成员数据访问层
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
public interface PelotonTeamMemberRepository extends JpaRepository<PelotonTeamMember, Long> {

    /**
     * 查询某车队的所有成员
     *
     * @param teamId 车队ID
     * @return 成员列表
     */
    List<PelotonTeamMember> findByTeamIdOrderByJoinDateAsc(Long teamId);

    /**
     * 查询某用户加入的所有车队
     *
     * @param userId 用户ID
     * @return 车队成员记录列表
     */
    List<PelotonTeamMember> findByUserId(Long userId);

    /**
     * 查询某用户是否已加入某车队
     *
     * @param teamId 车队ID
     * @param userId 用户ID
     * @return 成员记录
     */
    Optional<PelotonTeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    /**
     * 统计某车队的成员数量
     *
     * @param teamId 车队ID
     * @return 成员数量
     */
    long countByTeamId(Long teamId);
}
