package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamPostInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * TeamPostInteractionRepository
 * 车队动态互动数据访问层（映射 team_post_interaction 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
public interface TeamPostInteractionRepository extends JpaRepository<TeamPostInteraction, Long> {

    /**
     * 查询某用户对某条动态的指定类型互动记录（正常状态）
     *
     * @param postId 动态ID
     * @param userId 用户ID
     * @param type   互动类型
     * @param status 状态值
     * @return 互动记录
     */
    Optional<TeamPostInteraction> findByPostIdAndUserIdAndTypeAndStatus(
            Long postId, Long userId, Integer type, Integer status);

    /**
     * 查询某条动态下某类型的所有正常互动记录
     *
     * @param postId 动态ID
     * @param type   互动类型
     * @param status 状态值
     * @return 互动记录列表
     */
    List<TeamPostInteraction> findByPostIdAndTypeAndStatus(
            Long postId, Integer type, Integer status);

    /**
     * 批量查询多个动态下某类型的所有正常互动记录
     *
     * @param postIds 动态ID列表
     * @param type    互动类型
     * @param status  状态值
     * @return 互动记录列表
     */
    @Query("SELECT i FROM TeamPostInteraction i WHERE i.postId IN :postIds AND i.type = :type AND i.status = :status ORDER BY i.createdAt ASC")
    List<TeamPostInteraction> findByPostIdInAndTypeAndStatus(
            @Param("postIds") List<Long> postIds,
            @Param("type") Integer type,
            @Param("status") Integer status);

    /**
     * 查询某用户对多个动态的点赞记录（用于判断当前用户是否已点赞）
     *
     * @param postIds 动态ID列表
     * @param userId  用户ID
     * @param type    互动类型
     * @param status  状态值
     * @return 互动记录列表
     */
    @Query("SELECT i FROM TeamPostInteraction i WHERE i.postId IN :postIds AND i.userId = :userId AND i.type = :type AND i.status = :status")
    List<TeamPostInteraction> findByPostIdInAndUserIdAndTypeAndStatus(
            @Param("postIds") List<Long> postIds,
            @Param("userId") Long userId,
            @Param("type") Integer type,
            @Param("status") Integer status);

    /**
     * 查询某条正常状态的互动记录
     *
     * @param id     互动ID
     * @param status 状态值
     * @return 互动记录
     */
    Optional<TeamPostInteraction> findByIdAndStatus(Long id, Integer status);

    /**
     * 批量将某车队所有正常互动标记为已删除（解散车队时使用）
     */
    @Modifying
    @Query("UPDATE TeamPostInteraction i SET i.status = :newStatus WHERE i.postId IN (SELECT p.id FROM TeamPost p WHERE p.teamId = :teamId) AND i.status = :status")
    void updateStatusByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") Integer status, @Param("newStatus") Integer newStatus);

    /**
     * 物理删除某车队所有互动（I6: 解散车队时使用）
     */
    @Modifying
    @Query("DELETE FROM TeamPostInteraction i WHERE i.postId IN (SELECT p.id FROM TeamPost p WHERE p.teamId = :teamId)")
    void deleteByTeamId(@Param("teamId") Long teamId);
}
