package com.starrysky.aries.peloton.repository;

import com.starrysky.aries.entity.TeamPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * TeamPostRepository
 * 车队动态数据访问层（映射 team_post 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
public interface TeamPostRepository extends JpaRepository<TeamPost, Long> {

    /**
     * 分页查询某车队的正常动态（按发布时间倒序）
     *
     * @param teamId   车队ID
     * @param status   状态值
     * @param pageable 分页参数
     * @return 动态分页列表
     */
    @Query("SELECT p FROM TeamPost p WHERE p.teamId = :teamId AND p.status = :status ORDER BY p.createdAt DESC")
    Page<TeamPost> findActivePostsByTeamId(
            @Param("teamId") Long teamId,
            @Param("status") Integer status,
            Pageable pageable);

    /**
     * 查询某条正常状态的动态
     *
     * @param id     动态ID
     * @param status 状态值
     * @return 动态（如存在且状态匹配）
     */
    Optional<TeamPost> findByIdAndStatus(Long id, Integer status);

    /**
     * 原子递增点赞数（C2: 避免并发读写覆盖）
     */
    @Modifying
    @Query("UPDATE TeamPost p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    /**
     * 原子递减点赞数（最低0，C2: 避免并发读写覆盖）
     */
    @Modifying
    @Query("UPDATE TeamPost p SET p.likeCount = GREATEST(p.likeCount - 1, 0) WHERE p.id = :postId")
    void decrementLikeCount(@Param("postId") Long postId);

    /**
     * 原子递增评论数（C2: 避免并发读写覆盖）
     */
    @Modifying
    @Query("UPDATE TeamPost p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);

    /**
     * 原子递减评论数（最低0，C2: 避免并发读写覆盖）
     */
    @Modifying
    @Query("UPDATE TeamPost p SET p.commentCount = GREATEST(p.commentCount - 1, 0) WHERE p.id = :postId")
    void decrementCommentCount(@Param("postId") Long postId);

    /**
     * 批量将某车队所有正常动态标记为已删除（解散车队时使用）
     */
    @Modifying
    @Query("UPDATE TeamPost p SET p.status = :newStatus WHERE p.teamId = :teamId AND p.status = :status")
    void updateStatusByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") Integer status, @Param("newStatus") Integer newStatus);

    /**
     * 物理删除某车队所有动态（I6: 解散车队时使用）
     */
    @Modifying
    @Query("DELETE FROM TeamPost p WHERE p.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);
}
