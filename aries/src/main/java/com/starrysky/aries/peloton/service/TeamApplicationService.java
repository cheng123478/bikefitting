package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamJoinApplication;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamJoinApplicationRepository;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.schema.PelotonTeamMemberVO;
import com.starrysky.aries.util.PelotonTeamUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TeamApplicationService
 * 车队加入申请管理服务（申请、审核、拒绝）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Slf4j
@Service
public class TeamApplicationService {

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private TeamJoinApplicationRepository applicationRepository;

    @Resource
    private TeamMemberService teamMemberService;

    @Resource
    private com.starrysky.aries.user.repository.UserRepository userRepository;

    /**
     * 获取车队待审核申请列表（仅队长/管理员可查看）
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     * @return 申请列表
     */
    public List<PelotonTeamMemberVO> getPendingApplications(Long teamId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        teamMemberService.assertTeamAdmin(team, user);

        List<TeamJoinApplication> applications = applicationRepository
                .findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, TeamJoinApplication.STATUS_PENDING);
        Map<Long, User> userMap = PelotonTeamUtil.buildUserMap(applications, TeamJoinApplication::getApplicantId, userRepository);

        return applications.stream()
                .map(a -> buildApplicationVO(a, userMap.get(a.getApplicantId())))
                .collect(Collectors.toList());
    }

    /**
     * 申请加入车队（创建待审核记录）
     *
     * @param teamId 车队ID
     * @param reason 申请理由
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyJoinTeam(Long teamId, String reason, User user) {
        pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        String trimmedReason = reason != null ? reason.trim() : "";
        if (!StringUtils.hasText(trimmedReason)) {
            throw new BizException(400, "请填写申请理由");
        }

        applicationRepository.findByTeamIdAndApplicantIdAndStatus(teamId, user.getId(), TeamJoinApplication.STATUS_PENDING)
                .ifPresent(a -> {
                    throw new BizException(400, "您已提交过申请，请等待审核");
                });

        teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                .ifPresent(m -> {
                    throw new BizException(400, "您已是该车队成员，无需重复申请");
                });

        TeamJoinApplication application = TeamJoinApplication.builder()
                .teamId(teamId)
                .applicantId(user.getId())
                .reason(trimmedReason)
                .status(TeamJoinApplication.STATUS_PENDING)
                .build();
        applicationRepository.save(application);
    }

    /**
     * 同意加入申请（管理员/队长操作）
     *
     * @param applicationId 申请记录ID
     * @param admin         操作人（需为队长或管理员）
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveJoin(Long applicationId, User admin) {
        TeamJoinApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BizException(404, "申请记录不存在"));
        if (application.getStatus() != TeamJoinApplication.STATUS_PENDING) {
            throw new BizException(400, "该申请已被处理");
        }

        PelotonTeam team = pelotonTeamRepository.findById(application.getTeamId())
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        teamMemberService.assertTeamAdmin(team, admin);

        // 二次校验：申请人是否已通过其他途径成为成员
        var existingMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(
                team.getId(), application.getApplicantId(), TeamMember.STATUS_ACTIVE);
        if (existingMember.isPresent()) {
            // 申请人已是成员，仅更新申请状态为已通过，不重复添加成员
            application.setStatus(TeamJoinApplication.STATUS_APPROVED);
            application.setReviewerId(admin.getId());
            applicationRepository.save(application);
            log.info("用户 {} 审批通过申请 {}（车队 {}），申请人已是成员，仅更新申请状态",
                    admin.getId(), applicationId, team.getId());
            return;
        }

        application.setStatus(TeamJoinApplication.STATUS_APPROVED);
        application.setReviewerId(admin.getId());
        applicationRepository.save(application);

        TeamMember member = TeamMember.builder()
                .teamId(application.getTeamId())
                .userId(application.getApplicantId())
                .role(TeamMember.ROLE_MEMBER)
                .tag(TeamMember.DEFAULT_TAG)
                .joinDate(LocalDate.now())
                .status(TeamMember.STATUS_ACTIVE)
                .build();
        teamMemberRepository.save(member);

        teamMemberService.incrementMemberCount(team.getId());
        log.info("用户 {} 审批通过申请 {}，申请人 {} 加入车队 {}",
                admin.getId(), applicationId, application.getApplicantId(), team.getId());
    }

    /**
     * 拒绝加入申请（管理员/队长操作）
     *
     * @param applicationId 申请记录ID
     * @param admin         操作人（需为队长或管理员）
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectJoin(Long applicationId, User admin) {
        TeamJoinApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BizException(404, "申请记录不存在"));
        if (application.getStatus() != TeamJoinApplication.STATUS_PENDING) {
            throw new BizException(400, "该申请已被处理");
        }

        PelotonTeam team = pelotonTeamRepository.findById(application.getTeamId())
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        teamMemberService.assertTeamAdmin(team, admin);

        application.setStatus(TeamJoinApplication.STATUS_REJECTED);
        application.setReviewerId(admin.getId());
        applicationRepository.save(application);
        log.info("用户 {} 拒绝了加入申请 {}（车队 {}）", admin.getId(), applicationId, team.getId());
    }


    private PelotonTeamMemberVO buildApplicationVO(TeamJoinApplication app, User user) {
        PelotonTeamMemberVO.PelotonTeamMemberVOBuilder builder = PelotonTeamMemberVO.builder()
                .id(app.getId())
                .userId(app.getApplicantId())
                .reason(app.getReason())
                .joinDate(app.getCreatedAt());

        if (user != null) {
            builder.name(user.getName());
            builder.avatar(user.getAvatar());
        }

        return builder.build();
    }
}
