package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.entity.TeamNotice;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.notification.enums.NotificationType;
import com.starrysky.aries.notification.service.NotificationService;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.repository.TeamNoticeRepository;
import com.starrysky.aries.peloton.schema.CreateNoticeDTO;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.peloton.schema.TeamNoticeVO;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TeamNoticeService
 * 车队公告服务（发布、编辑、删除、公告栏查询）
 *
 * <h3>可见性说明</h3>
 * <p>公告栏仅对队内成员可见，所有公告接口均需登录。</p>
 *
 * <h3>权限说明</h3>
 * <ul>
 *   <li>发布公告：队长或管理员</li>
 *   <li>编辑公告：公告发布者本人，或队长/管理员</li>
 *   <li>删除公告：公告发布者本人，或队长/管理员</li>
 *   <li>查看公告栏：仅队内成员</li>
 * </ul>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Slf4j
@Service
public class TeamNoticeService {

    /** 公告栏默认每页条数 */
    private static final int NOTICE_PAGE_SIZE = 10;

    @Resource
    private TeamNoticeRepository teamNoticeRepository;

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
     * 发布公告
     * <p>
     * 仅队长或管理员可发布。标题和内容会做 XSS 过滤。
     *
     * @param teamId 车队ID
     * @param dto    发布参数（title, content）
     * @param user   当前登录用户
     * @return 公告视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamNoticeVO publishNotice(Long teamId, CreateNoticeDTO dto, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        // 校验权限：队长或管理员（W4: 复用 TeamMemberService.assertTeamAdmin）
        teamMemberService.assertTeamAdmin(team, user);

        // 组装公告实体（用户可见文本做 XSS 过滤）
        TeamNotice notice = TeamNotice.builder()
                .teamId(teamId)
                .authorId(user.getId())
                .title(HtmlUtils.htmlEscape(dto.getTitle().trim()))
                .content(HtmlUtils.htmlEscape(dto.getContent().trim()))
                .status(TeamNotice.STATUS_ACTIVE)
                .build();
        notice = teamNoticeRepository.save(notice);

        // 通知所有队内成员：新公告发布（排除发布者自己）
        List<Long> memberIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE)
                .stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !uid.equals(user.getId()))
                .collect(Collectors.toList());
        notificationService.sendBatch(memberIds,
                NotificationType.NEW_NOTICE,
                "新公告发布",
                "管理员发布了新公告「" + notice.getTitle() + "」",
                teamId, notice.getId());

        log.debug("用户 {} 在车队 {} 中发布了公告（id={}）", user.getId(), teamId, notice.getId());
        return convertToVO(notice, user);
    }

    /**
     * 编辑公告
     * <p>
     * 公告发布者本人、队长或管理员可编辑。
     *
     * @param teamId   车队ID
     * @param noticeId 公告ID
     * @param dto      编辑参数（title, content）
     * @param user     当前登录用户
     * @return 更新后的公告视图对象
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamNoticeVO updateNotice(Long teamId, Long noticeId, CreateNoticeDTO dto, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        TeamNotice notice = teamNoticeRepository.findByIdAndStatus(noticeId, TeamNotice.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "公告不存在"));

        // 先校验归属：公告必须属于当前车队
        if (!Objects.equals(notice.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的公告");
        }

        // 校验权限：发布者本人、队长或管理员
        assertNoticeOperatorOrAdmin(team, notice, user, "仅公告发布者、队长或管理员可编辑公告");

        // 更新公告（用户可见文本做 XSS 过滤）
        notice.setTitle(HtmlUtils.htmlEscape(dto.getTitle().trim()));
        notice.setContent(HtmlUtils.htmlEscape(dto.getContent().trim()));
        notice = teamNoticeRepository.save(notice);

        // 通知所有队内成员：公告已编辑（排除编辑者自己）
        List<Long> memberIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE)
                .stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !uid.equals(user.getId()))
                .collect(Collectors.toList());
        notificationService.sendBatch(memberIds,
                NotificationType.EDIT_NOTICE,
                "公告已编辑",
                "公告「" + notice.getTitle() + "」已更新内容",
                teamId, noticeId);

        log.debug("用户 {} 在车队 {} 中编辑了公告（id={}）", user.getId(), teamId, noticeId);
        User author = userRepository.findById(notice.getAuthorId()).orElse(null);
        return convertToVO(notice, author);
    }

    /**
     * 删除公告（软删除，将 status 置为已删除）
     * <p>
     * 公告发布者本人、队长或管理员可删除。
     *
     * @param teamId   车队ID
     * @param noticeId 公告ID
     * @param user     当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long teamId, Long noticeId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        TeamNotice notice = teamNoticeRepository.findByIdAndStatus(noticeId, TeamNotice.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "公告不存在"));

        // 先校验归属：公告必须属于当前车队
        if (!Objects.equals(notice.getTeamId(), teamId)) {
            throw new BizException(403, "无权操作该车队的公告");
        }

        // 校验权限：发布者本人、队长或管理员
        assertNoticeOperatorOrAdmin(team, notice, user, "仅公告发布者、队长或管理员可删除公告");

        notice.setStatus(TeamNotice.STATUS_DELETED);
        teamNoticeRepository.save(notice);

        log.debug("用户 {} 在车队 {} 中删除了公告（id={}）", user.getId(), teamId, noticeId);
    }

    /**
     * 获取公告栏（分页查询车队的正常公告，按发布时间倒序）
     * <p>
     * 仅队内成员可访问。
     *
     * @param teamId 车队ID
     * @param page   页码（从1开始）
     * @param size   每页条数（默认10）
     * @param user   当前登录用户
     * @return 公告分页列表
     */
    @Transactional(readOnly = true)
    public PageResponse<TeamNoticeVO> getNoticeBoard(Long teamId, Integer page, Integer size, User user) {
        // C3: 添加成员身份校验，仅队内成员可查看公告栏
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        teamMemberService.assertTeamMember(team, user);

        int currentPage = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? Math.min(size, 50) : NOTICE_PAGE_SIZE;
        PageRequest pageRequest = PageRequest.of(currentPage - 1, pageSize);

        Page<TeamNotice> noticePage = teamNoticeRepository.findActiveNoticesByTeamId(
                teamId, TeamNotice.STATUS_ACTIVE, pageRequest);

        // 批量查询发布者信息，避免 N+1 查询
        List<Long> authorIds = noticePage.getContent().stream()
                .map(TeamNotice::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TeamNoticeVO> records = noticePage.getContent().stream()
                .map(n -> convertToVO(n, userMap.get(n.getAuthorId())))
                .collect(Collectors.toList());

        return PageResponse.<TeamNoticeVO>builder()
                .records(records)
                .total(noticePage.getTotalElements())
                .page(currentPage)
                .size(pageSize)
                .pages(noticePage.getTotalPages())
                .build();
    }

    // ============================ 私有方法 ============================

    /**
     * 公告实体 -> VO
     *
     * @param notice 公告实体
     * @param author 发布者（可为 null）
     * @return 公告视图对象
     */
    private TeamNoticeVO convertToVO(TeamNotice notice, User author) {
        TeamNoticeVO.TeamNoticeVOBuilder builder = TeamNoticeVO.builder()
                .id(notice.getId())
                .teamId(notice.getTeamId())
                .authorId(notice.getAuthorId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt());

        if (author != null) {
            builder.authorName(author.getName());
            builder.authorAvatar(author.getAvatar());
        }

        return builder.build();
    }

    /**
     * 校验当前用户是否有权操作该公告（发布者本人、队长或管理员）
     *
     * @param team    车队实体
     * @param notice  公告实体
     * @param user    当前用户
     * @param message 权限不足时的错误信息
     */
    private void assertNoticeOperatorOrAdmin(PelotonTeam team, TeamNotice notice, User user, String message) {
        // 公告发布者本人
        if (Objects.equals(notice.getAuthorId(), user.getId())) {
            return;
        }
        // 队长或管理员（W4: 复用 TeamMemberService.assertTeamAdmin，避免重复代码）
        teamMemberService.assertTeamAdmin(team, user);
    }
}
