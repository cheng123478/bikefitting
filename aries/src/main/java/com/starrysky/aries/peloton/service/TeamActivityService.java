package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamActivity;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.notification.enums.NotificationType;
import com.starrysky.aries.notification.service.NotificationService;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamActivityRepository;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.schema.CreateActivityDTO;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.peloton.schema.TeamActivityVO;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TeamActivityService
 * 车队活动服务（发布、编辑、删除、活动列表查询）
 *
 * <h3>可见性说明</h3>
 * <p>活动列表对所有用户可见（无需队内成员身份），但发布/编辑/删除需权限。</p>
 *
 * <h3>权限说明</h3>
 * <ul>
 *   <li>发布活动：队长或管理员</li>
 *   <li>编辑活动：活动发布者本人，或队长/管理员</li>
 *   <li>删除活动：活动发布者本人，或队长/管理员</li>
 *   <li>查看活动列表：无需权限（公开）</li>
 * </ul>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Slf4j
@Service
public class TeamActivityService {

    /** 活动列表默认每页条数 */
    private static final int ACTIVITY_PAGE_SIZE = 10;

    @Resource
    private TeamActivityRepository teamActivityRepository;

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private TeamMemberService teamMemberService;

    @Resource
    private NotificationService notificationService;

    /**
     * 发布活动
     * <p>
     * 仅队长或管理员可发布。标题和描述会做 XSS 过滤。
     *
     * @param teamId 车队ID
     * @param dto    发布参数
     * @param user   当前登录用户
     * @return 活动视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamActivityVO publishActivity(Long teamId, CreateActivityDTO dto, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        // 校验权限：队长或管理员
        teamMemberService.assertTeamAdmin(team, user);

        // 解析日期和时间
        LocalDate eventDate = LocalDate.parse(dto.getEventDate());
        LocalTime eventTime = StringUtils.hasText(dto.getEventTime())
                ? LocalTime.parse(dto.getEventTime()) : null;

        // 组装活动实体（用户可见文本做 XSS 过滤）
        TeamActivity activity = TeamActivity.builder()
                .teamId(teamId)
                .authorId(user.getId())
                .title(HtmlUtils.htmlEscape(dto.getTitle().trim()))
                .activityType(dto.getActivityType())
                .eventDate(eventDate)
                .eventTime(eventTime)
                .location(HtmlUtils.htmlEscape(dto.getLocation().trim()))
                .route(StringUtils.hasText(dto.getRoute()) ? HtmlUtils.htmlEscape(dto.getRoute().trim()) : null)
                .difficulty(dto.getDifficulty())
                .distance(dto.getDistance())
                .maxParticipants(dto.getMaxParticipants())
                .description(HtmlUtils.htmlEscape(dto.getDescription().trim()))
                .coverImage(StringUtils.hasText(dto.getCoverImage()) ? HtmlUtils.htmlEscape(dto.getCoverImage().trim()) : null)
                .participantCount(0)
                .status(TeamActivity.STATUS_ACTIVE)
                .build();
        activity = teamActivityRepository.save(activity);

        // 通知所有队内成员：新活动发布（排除发布者自己）
        List<Long> memberIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE)
                .stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !uid.equals(user.getId()))
                .collect(Collectors.toList());
        notificationService.sendBatch(memberIds,
                NotificationType.NEW_ACTIVITY,
                "新活动发布",
                "队长「" + user.getName() + "」发布了新活动「" + activity.getTitle() + "」，快来报名吧！",
                teamId, activity.getId());

        log.debug("用户 {} 在车队 {} 中发布了活动（id={}）", user.getId(), teamId, activity.getId());
        return convertToVO(activity, user);
    }

    /**
     * 编辑活动
     * <p>
     * 活动发布者本人、队长或管理员可编辑。
     *
     * @param teamId     车队ID
     * @param activityId 活动ID
     * @param dto        编辑参数
     * @param user       当前登录用户
     * @return 更新后的活动视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamActivityVO updateActivity(Long teamId, Long activityId, CreateActivityDTO dto, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        TeamActivity activity = teamActivityRepository.findByIdAndStatus(activityId, TeamActivity.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "活动不存在"));

        // 先校验归属：活动必须属于当前车队
        if (!Objects.equals(activity.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的活动");
        }

        // 校验权限：发布者本人、队长或管理员
        assertActivityOperatorOrAdmin(team, activity, user);

        // 解析日期和时间
        LocalDate eventDate = LocalDate.parse(dto.getEventDate());
        LocalTime eventTime = StringUtils.hasText(dto.getEventTime())
                ? LocalTime.parse(dto.getEventTime()) : null;

        // 更新活动（用户可见文本做 XSS 过滤）
        activity.setTitle(HtmlUtils.htmlEscape(dto.getTitle().trim()));
        activity.setActivityType(dto.getActivityType());
        activity.setEventDate(eventDate);
        activity.setEventTime(eventTime);
        activity.setLocation(HtmlUtils.htmlEscape(dto.getLocation().trim()));
        activity.setRoute(StringUtils.hasText(dto.getRoute()) ? HtmlUtils.htmlEscape(dto.getRoute().trim()) : null);
        activity.setDifficulty(dto.getDifficulty());
        activity.setDistance(dto.getDistance());
        activity.setMaxParticipants(dto.getMaxParticipants());
        activity.setDescription(HtmlUtils.htmlEscape(dto.getDescription().trim()));
        activity.setCoverImage(StringUtils.hasText(dto.getCoverImage()) ? HtmlUtils.htmlEscape(dto.getCoverImage().trim()) : null);
        activity = teamActivityRepository.save(activity);

        // 通知所有队内成员：活动已编辑（排除操作者自己）
        List<Long> memberIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE)
                .stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !uid.equals(user.getId()))
                .collect(Collectors.toList());
        notificationService.sendBatch(memberIds,
                NotificationType.EDIT_ACTIVITY,
                "活动已更新",
                "「" + activity.getTitle() + "」活动信息已更新",
                teamId, activityId);

        log.debug("用户 {} 在车队 {} 中编辑了活动（id={}）", user.getId(), teamId, activityId);
        User author = userRepository.findById(activity.getAuthorId()).orElse(null);
        return convertToVO(activity, author);
    }

    /**
     * 删除活动（软删除，将 status 置为已删除）
     * <p>
     * 活动发布者本人、队长或管理员可删除。
     *
     * @param teamId     车队ID
     * @param activityId 活动ID
     * @param user       当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long teamId, Long activityId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        TeamActivity activity = teamActivityRepository.findByIdAndStatus(activityId, TeamActivity.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "活动不存在"));

        // 先校验归属：活动必须属于当前车队
        if (!Objects.equals(activity.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的活动");
        }

        // 校验权限：发布者本人、队长或管理员
        assertActivityOperatorOrAdmin(team, activity, user);

        activity.setStatus(TeamActivity.STATUS_DELETED);
        teamActivityRepository.save(activity);

        // 通知所有队内成员：活动已删除（排除操作者自己）
        List<Long> memberIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE)
                .stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !uid.equals(user.getId()))
                .collect(Collectors.toList());
        notificationService.sendBatch(memberIds,
                NotificationType.DELETE_ACTIVITY,
                "活动已取消",
                "「" + activity.getTitle() + "」活动已被取消",
                teamId, activityId);

        log.debug("用户 {} 在车队 {} 中删除了活动（id={}）", user.getId(), teamId, activityId);
    }

    /**
     * 获取活动列表（分页查询车队的正常活动，按活动日期倒序）
     * <p>
     * 公开接口，无需队内成员身份即可查看。
     *
     * @param teamId 车队ID
     * @param page   页码（从1开始）
     * @param size   每页条数（默认10）
     * @return 活动分页列表
     */
    @Transactional(readOnly = true)
    public PageResponse<TeamActivityVO> getActivities(Long teamId, Integer page, Integer size) {
        // 校验车队存在
        if (!pelotonTeamRepository.existsById(teamId)) {
            throw new BizException(404, "车队不存在");
        }

        int currentPage = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? Math.min(size, 50) : ACTIVITY_PAGE_SIZE;
        PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);

        Page<TeamActivity> activityPage = teamActivityRepository.findActiveActivitiesByTeamId(
                teamId, TeamActivity.STATUS_ACTIVE, pageRequest);

        // 批量查询发布者信息，避免 N+1 查询
        List<Long> authorIds = activityPage.getContent().stream()
                .map(TeamActivity::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TeamActivityVO> records = activityPage.getContent().stream()
                .map(a -> convertToVO(a, userMap.get(a.getAuthorId())))
                .collect(Collectors.toList());

        return PageResponse.<TeamActivityVO>builder()
                .records(records)
                .total(activityPage.getTotalElements())
                .page(currentPage)
                .size(pageSize)
                .pages(activityPage.getTotalPages())
                .build();
    }

    // ============================ 私有方法 ============================

    /**
     * 活动实体 -> VO
     *
     * @param activity 活动实体
     * @param author   发布者（可为 null）
     * @return 活动视图对象
     */
    private TeamActivityVO convertToVO(TeamActivity activity, User author) {
        TeamActivityVO.TeamActivityVOBuilder builder = TeamActivityVO.builder()
                .id(activity.getId())
                .teamId(activity.getTeamId())
                .authorId(activity.getAuthorId())
                .title(activity.getTitle())
                .activityType(activity.getActivityType())
                .eventDate(activity.getEventDate())
                .eventTime(activity.getEventTime())
                .location(activity.getLocation())
                .route(activity.getRoute())
                .difficulty(activity.getDifficulty())
                .distance(activity.getDistance())
                .maxParticipants(activity.getMaxParticipants())
                .description(activity.getDescription())
                .coverImage(activity.getCoverImage())
                .participantCount(activity.getParticipantCount())
                .status(activity.getStatus())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt());

        if (author != null) {
            builder.authorName(author.getName());
            builder.authorAvatar(author.getAvatar());
        }

        return builder.build();
    }

    /**
     * 校验当前用户是否有权操作该活动（发布者本人、队长或管理员）
     *
     * @param team     车队实体
     * @param activity 活动实体
     * @param user     当前用户
     */
    private void assertActivityOperatorOrAdmin(PelotonTeam team, TeamActivity activity, User user) {
        // 活动发布者本人
        if (Objects.equals(activity.getAuthorId(), user.getId())) {
            return;
        }
        // 队长或管理员
        teamMemberService.assertTeamAdmin(team, user);
    }
}
