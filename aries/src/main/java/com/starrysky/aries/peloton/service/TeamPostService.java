package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.*;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.notification.enums.NotificationType;
import com.starrysky.aries.notification.service.NotificationService;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.repository.TeamPostInteractionRepository;
import com.starrysky.aries.peloton.repository.TeamPostRepository;
import com.starrysky.aries.peloton.schema.*;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TeamPostService
 * 车队动态服务（发布、查看、删除动态；点赞/取消点赞；发布/删除评论）
 *
 * <h3>可见性说明</h3>
 * <p>动态列表仅对队内成员可见，所有动态接口均需登录。</p>
 *
 * <h3>权限说明</h3>
 * <ul>
 *   <li>发布动态：仅队内成员</li>
 *   <li>查看动态：仅队内成员</li>
 *   <li>删除动态：仅动态发布者本人</li>
 *   <li>点赞/取消点赞：仅队内成员</li>
 *   <li>发布评论：仅队内成员</li>
 *   <li>删除评论：仅评论发布者本人</li>
 * </ul>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Slf4j
@Service
public class TeamPostService {

    /** 动态列表默认每页条数 */
    private static final int POST_PAGE_SIZE = 10;

    @Resource
    private TeamPostRepository teamPostRepository;

    @Resource
    private TeamPostInteractionRepository interactionRepository;

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private NotificationService notificationService;

    /**
     * 发布动态
     * <p>
     * 仅队内成员可发布。文本内容做 XSS 过滤。
     *
     * @param teamId 车队ID
     * @param dto    发布参数（content, images）
     * @param user   当前登录用户
     * @return 动态视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamPostVO publishPost(Long teamId, CreatePostDTO dto, User user) {
        // 校验车队存在 & 当前用户是队内成员
        assertTeamMember(teamId, user, "仅队内成员可发布动态");

        // XSS 过滤文本内容（允许 null）
        String rawContent = dto.getContent() != null ? dto.getContent().trim() : "";
        String safeContent = rawContent.isEmpty() ? "" : HtmlUtils.htmlEscape(rawContent);

        // 过滤图片 URL（防止 null 列表）
        List<String> safeImages = dto.getImages() != null
                ? dto.getImages().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList())
                : null;

        // 图文至少有一项
        if (safeContent.isEmpty() && (safeImages == null || safeImages.isEmpty())) {
            throw new BizException(400, "动态内容和图片不能同时为空");
        }

        TeamPost post = TeamPost.builder()
                .teamId(teamId)
                .authorId(user.getId())
                .content(safeContent)
                .images(safeImages)
                .likeCount(0)
                .commentCount(0)
                .status(TeamPost.STATUS_ACTIVE)
                .build();
        post = teamPostRepository.save(post);

        log.debug("用户 {} 在车队 {} 中发布了动态（id={}）", user.getId(), teamId, post.getId());
        return convertToVO(post, user, Collections.emptyList(), false);
    }

    /**
     * 分页获取车队动态列表（含点赞状态与评论）
     * <p>
     * 仅队内成员可访问。批量查询评论与点赞状态以避免 N+1。
     *
     * @param teamId 车队ID
     * @param page   页码（从1开始）
     * @param size   每页条数（默认10，最大50）
     * @param user   当前登录用户
     * @return 动态分页列表
     */
    @Transactional(readOnly = true)
    public PageResponse<TeamPostVO> getPosts(Long teamId, Integer page, Integer size, User user) {
        // 校验车队存在 & 当前用户是队内成员
        assertTeamMember(teamId, user, "仅队内成员可查看动态");

        int currentPage = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? Math.min(size, 50) : POST_PAGE_SIZE;
        PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);

        Page<TeamPost> postPage = teamPostRepository.findActivePostsByTeamId(
                teamId, TeamPost.STATUS_ACTIVE, pageRequest);

        List<TeamPost> posts = postPage.getContent();
        if (posts.isEmpty()) {
            return PageResponse.<TeamPostVO>builder()
                    .records(Collections.emptyList())
                    .total(postPage.getTotalElements())
                    .page(currentPage)
                    .size(pageSize)
                    .pages(postPage.getTotalPages())
                    .build();
        }

        // 批量查询所有涉及的作者信息
        List<Long> authorIds = posts.stream()
                .map(TeamPost::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询所有动态的评论
        List<Long> postIds = posts.stream().map(TeamPost::getId).collect(Collectors.toList());
        List<TeamPostInteraction> allComments = interactionRepository
                .findByPostIdInAndTypeAndStatus(postIds, TeamPostInteraction.TYPE_COMMENT, TeamPostInteraction.STATUS_ACTIVE);

        // 收集所有评论者ID并批量查询
        List<Long> commenterIds = allComments.stream()
                .map(TeamPostInteraction::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> commenterMap = userRepository.findAllById(commenterIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 按 post_id 分组评论
        Map<Long, List<TeamPostInteraction>> commentsByPost = allComments.stream()
                .collect(Collectors.groupingBy(TeamPostInteraction::getPostId));

        // 批量查询当前用户的点赞状态
        Set<Long> likedPostIds = interactionRepository
                .findByPostIdInAndUserIdAndTypeAndStatus(postIds, user.getId(),
                        TeamPostInteraction.TYPE_LIKE, TeamPostInteraction.STATUS_ACTIVE)
                .stream()
                .map(TeamPostInteraction::getPostId)
                .collect(Collectors.toSet());

        // 组装 VO 列表
        List<TeamPostVO> records = posts.stream()
                .map(post -> {
                    List<TeamPostInteraction> postComments = commentsByPost.getOrDefault(post.getId(), Collections.emptyList());
                    List<TeamPostCommentVO> commentVOs = postComments.stream()
                            .map(c -> convertCommentToVO(c, commenterMap.get(c.getUserId())))
                            .collect(Collectors.toList());
                    boolean liked = likedPostIds.contains(post.getId());
                    User author = authorMap.get(post.getAuthorId());
                    return convertToVO(post, author, commentVOs, liked);
                })
                .collect(Collectors.toList());

        return PageResponse.<TeamPostVO>builder()
                .records(records)
                .total(postPage.getTotalElements())
                .page(currentPage)
                .size(pageSize)
                .pages(postPage.getTotalPages())
                .build();
    }

    /**
     * 删除动态（软删除，仅发布者可操作）
     *
     * @param teamId 车队ID
     * @param postId 动态ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long teamId, Long postId, User user) {
        TeamPost post = teamPostRepository.findByIdAndStatus(postId, TeamPost.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "动态不存在"));

        // 校验归属归属
        if (!Objects.equals(post.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的动态");
        }

        // 仅发布者可删除
        if (!Objects.equals(post.getAuthorId(), user.getId())) {
            throw new BizException(403, "仅动态发布者可删除该动态");
        }

        post.setStatus(TeamPost.STATUS_DELETED);
        teamPostRepository.save(post);

        log.debug("用户 {} 在车队 {} 中删除了动态（id={}）", user.getId(), teamId, postId);
    }

    /**
     * 点赞/取消点赞（切换模式）
     * <p>
     * 若当前用户已点赞则取消点赞（软删除点赞记录，点赞数-1）；
     * 若未点赞则新增点赞记录（点赞数+1）。
     *
     * @param teamId 车队ID
     * @param postId 动态ID
     * @param user   当前登录用户
     * @return true=点赞成功，false=取消点赞成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long teamId, Long postId, User user) {
        // 校验队内成员
        assertTeamMember(teamId, user, "仅队内成员可点赞");

        TeamPost post = teamPostRepository.findByIdAndStatus(postId, TeamPost.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "动态不存在"));

        if (!Objects.equals(post.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的动态");
        }

        Optional<TeamPostInteraction> existingLike = interactionRepository
                .findByPostIdAndUserIdAndTypeAndStatus(
                        postId, user.getId(),
                        TeamPostInteraction.TYPE_LIKE, TeamPostInteraction.STATUS_ACTIVE);

        if (existingLike.isPresent()) {
            // 取消点赞（C2: 原子递减避免并发覆盖）
            TeamPostInteraction like = existingLike.get();
            like.setStatus(TeamPostInteraction.STATUS_DELETED);
            interactionRepository.save(like);
            teamPostRepository.decrementLikeCount(postId);
            log.debug("用户 {} 取消了对动态 {} 的点赞", user.getId(), postId);
            return false;
        } else {
            // 点赞（C2: 原子递增避免并发覆盖）
            TeamPostInteraction like = TeamPostInteraction.builder()
                    .postId(postId)
                    .userId(user.getId())
                    .type(TeamPostInteraction.TYPE_LIKE)
                    .status(TeamPostInteraction.STATUS_ACTIVE)
                    .build();
            interactionRepository.save(like);
            teamPostRepository.incrementLikeCount(postId);
            log.debug("用户 {} 对动态 {} 点赞", user.getId(), postId);

            // 通知动态作者：获得点赞（排除自己给自己点赞的情况）
            if (!Objects.equals(post.getAuthorId(), user.getId())) {
                String likerName = user.getName() != null ? user.getName() : "某用户";
                String postPreview = post.getContent() != null && post.getContent().length() > 20
                        ? post.getContent().substring(0, 20) + "..." : (post.getContent() != null ? post.getContent() : "图片动态");
                notificationService.send(post.getAuthorId(),
                        NotificationType.LIKE,
                        "获得点赞",
                        "「" + likerName + "」赞了您的动态「" + postPreview + "」",
                        post.getTeamId(), postId);
            }

            return true;
        }
    }

    /**
     * 发表评论
     * <p>
     * 仅队内成员可评论。评论内容做 XSS 过滤。
     *
     * @param teamId 车队ID
     * @param postId 动态ID
     * @param dto    评论参数（content）
     * @param user   当前登录用户
     * @return 评论视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamPostCommentVO addComment(Long teamId, Long postId, CreateCommentDTO dto, User user) {
        // 校验队内成员
        assertTeamMember(teamId, user, "仅队内成员可评论");

        TeamPost post = teamPostRepository.findByIdAndStatus(postId, TeamPost.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "动态不存在"));

        if (!Objects.equals(post.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的动态");
        }

        String safeContent = HtmlUtils.htmlEscape(dto.getContent().trim());

        TeamPostInteraction comment = TeamPostInteraction.builder()
                .postId(postId)
                .userId(user.getId())
                .type(TeamPostInteraction.TYPE_COMMENT)
                .content(safeContent)
                .status(TeamPostInteraction.STATUS_ACTIVE)
                .build();
        comment = interactionRepository.save(comment);

        // 更新冗余评论数（C2: 原子操作避免并发覆盖）
        teamPostRepository.incrementCommentCount(postId);

        log.debug("用户 {} 在动态 {} 下发表了评论（id={}）", user.getId(), postId, comment.getId());

        // 通知动态作者：新评论（排除自己评论自己的情况）
        if (!Objects.equals(post.getAuthorId(), user.getId())) {
            String commenterName = user.getName() != null ? user.getName() : "某用户";
            String commentPreview = safeContent.length() > 20
                    ? safeContent.substring(0, 20) + "..." : safeContent;
            notificationService.send(post.getAuthorId(),
                    NotificationType.COMMENT,
                    "新评论",
                    "「" + commenterName + "」评论了您的动态：「" + commentPreview + "」",
                    post.getTeamId(), postId);
        }

        return convertCommentToVO(comment, user);
    }

    /**
     * 删除评论（软删除，仅评论发布者可操作）
     *
     * @param teamId    车队ID
     * @param postId    动态ID
     * @param commentId 评论ID
     * @param user      当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long teamId, Long postId, Long commentId, User user) {
        TeamPostInteraction comment = interactionRepository.findByIdAndStatus(commentId, TeamPostInteraction.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "评论不存在"));

        // 校验评论归属动态
        if (!Objects.equals(comment.getPostId(), postId)) {
            throw new BizException(403, "评论不属于该动态");
        }

        // 校验评论类型
        if (comment.getType() != TeamPostInteraction.TYPE_COMMENT) {
            throw new BizException(400, "操作对象不是评论");
        }

        // 仅评论发布者可删除
        if (!Objects.equals(comment.getUserId(), user.getId())) {
            throw new BizException(403, "仅评论发布者可删除该评论");
        }

        comment.setStatus(TeamPostInteraction.STATUS_DELETED);
        interactionRepository.save(comment);

        // 更新冗余评论数（C2: 原子操作避免并发覆盖）
        teamPostRepository.decrementCommentCount(postId);

        log.debug("用户 {} 删除了动态 {} 下的评论（id={}）", user.getId(), postId, commentId);
    }

    // ============================ 私有方法 ============================

    /**
     * 动态实体 -> VO
     *
     * @param post     动态实体
     * @param author   发布者（可为 null）
     * @param comments 评论列表
     * @param liked    当前用户是否已点赞
     * @return 动态视图对象
     */
    private TeamPostVO convertToVO(TeamPost post, User author, List<TeamPostCommentVO> comments, boolean liked) {
        TeamPostVO.TeamPostVOBuilder builder = TeamPostVO.builder()
                .id(post.getId())
                .teamId(post.getTeamId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .images(post.getImages())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .comments(comments)
                .createdAt(post.getCreatedAt());

        if (author != null) {
            builder.authorName(author.getName());
            builder.authorAvatar(author.getAvatar());
        }

        return builder.build();
    }

    /**
     * 评论互动实体 -> VO
     *
     * @param interaction 评论互动实体
     * @param user        评论者（可为 null）
     * @return 评论视图对象
     */
    private TeamPostCommentVO convertCommentToVO(TeamPostInteraction interaction, User user) {
        TeamPostCommentVO.TeamPostCommentVOBuilder builder = TeamPostCommentVO.builder()
                .id(interaction.getId())
                .userId(interaction.getUserId())
                .content(interaction.getContent())
                .createdAt(interaction.getCreatedAt());

        if (user != null) {
            builder.userName(user.getName());
            builder.userAvatar(user.getAvatar());
        }

        return builder.build();
    }

    /**
     * 校验当前用户是否为车队活跃成员
     * <p>C1: 仅一次查询车队信息，避免重复查询</p>
     * <p>W8: 使用 captainId 判断队长身份</p>
     *
     * @param teamId  车队ID
     * @param user    当前用户
     * @param message 权限不足时的错误信息
     */
    private void assertTeamMember(Long teamId, User user, String message) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        // 车队队长自动视为成员（W8: 使用 captainId）
        if (Objects.equals(team.getCaptainId(), user.getId())) {
            return;
        }

        teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(403, message));
    }
}
