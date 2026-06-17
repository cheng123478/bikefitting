package com.starrysky.aries.peloton.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.peloton.schema.CreateCommentDTO;
import com.starrysky.aries.peloton.schema.CreatePostDTO;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.peloton.schema.TeamPostCommentVO;
import com.starrysky.aries.peloton.schema.TeamPostVO;
import com.starrysky.aries.peloton.service.TeamPostService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TeamPostController
 * 破风社-车队动态控制器
 *
 * <h3>认证边界说明</h3>
 * <ul>
 *   <li><b>需要登录（仅队内成员可操作）</b>：所有动态相关接口均需认证。</li>
 * </ul>
 *
 * <h3>接口说明</h3>
 * <ul>
 *   <li>POST   /{teamId}/posts               - 发布动态（仅队内成员）</li>
 *   <li>GET    /{teamId}/posts               - 获取动态列表（仅队内成员，分页）</li>
 *   <li>DELETE /{teamId}/posts/{postId}      - 删除动态（仅发布者）</li>
 *   <li>POST   /{teamId}/posts/{postId}/like - 点赞/取消点赞（仅队内成员，切换）</li>
 *   <li>POST   /{teamId}/posts/{postId}/comments           - 发表评论（仅队内成员）</li>
 *   <li>DELETE /{teamId}/posts/{postId}/comments/{commentId} - 删除评论（仅评论发布者）</li>
 * </ul>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@RestController
@RequestMapping("/team")
public class TeamPostController {

    @Resource
    private TeamPostService teamPostService;

    /**
     * 发布动态
     * <p>
     * 需要登录，仅队内成员可发布。
     *
     * @param teamId 车队ID
     * @param dto    发布参数（content, images）
     * @param user   当前登录用户
     * @return 动态视图对象
     */
    @PostMapping("/{teamId}/posts")
    public ResponseResult<TeamPostVO> publishPost(
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody CreatePostDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamPostService.publishPost(teamId, dto, user));
    }

    /**
     * 获取动态列表（分页，含点赞状态与评论）
     * <p>
     * 需要登录，仅队内成员可访问。
     *
     * @param teamId 车队ID
     * @param page   页码（从1开始，默认1）
     * @param size   每页条数（默认10，最大50）
     * @param user   当前登录用户
     * @return 动态分页列表
     */
    @GetMapping("/{teamId}/posts")
    public ResponseResult<PageResponse<TeamPostVO>> getPosts(
            @PathVariable("teamId") Long teamId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamPostService.getPosts(teamId, page, size, user));
    }

    /**
     * 删除动态（软删除）
     * <p>
     * 需要登录，仅动态发布者可操作。
     *
     * @param teamId 车队ID
     * @param postId 动态ID
     * @param user   当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/posts/{postId}")
    public ResponseResult<Void> deletePost(
            @PathVariable("teamId") Long teamId,
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal User user) {
        teamPostService.deletePost(teamId, postId, user);
        return ResponseResult.success();
    }

    /**
     * 点赞/取消点赞（切换模式）
     * <p>
     * 需要登录，仅队内成员可操作。
     * 若已点赞则取消（返回 liked=false），未点赞则点赞（返回 liked=true）。
     *
     * @param teamId 车队ID
     * @param postId 动态ID
     * @param user   当前登录用户
     * @return 点赞结果 { liked: true/false }
     */
    @PostMapping("/{teamId}/posts/{postId}/like")
    public ResponseResult<Map<String, Boolean>> toggleLike(
            @PathVariable("teamId") Long teamId,
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal User user) {
        boolean liked = teamPostService.toggleLike(teamId, postId, user);
        return ResponseResult.success(Map.of("liked", liked));
    }

    /**
     * 发表评论
     * <p>
     * 需要登录，仅队内成员可操作。
     *
     * @param teamId 车队ID
     * @param postId 动态ID
     * @param dto    评论参数（content）
     * @param user   当前登录用户
     * @return 评论视图对象
     */
    @PostMapping("/{teamId}/posts/{postId}/comments")
    public ResponseResult<TeamPostCommentVO> addComment(
            @PathVariable("teamId") Long teamId,
            @PathVariable("postId") Long postId,
            @Valid @RequestBody CreateCommentDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamPostService.addComment(teamId, postId, dto, user));
    }

    /**
     * 删除评论（软删除）
     * <p>
     * 需要登录，仅评论发布者可操作。
     *
     * @param teamId    车队ID
     * @param postId    动态ID
     * @param commentId 评论ID
     * @param user      当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/posts/{postId}/comments/{commentId}")
    public ResponseResult<Void> deleteComment(
            @PathVariable("teamId") Long teamId,
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal User user) {
        teamPostService.deleteComment(teamId, postId, commentId, user);
        return ResponseResult.success();
    }
}
