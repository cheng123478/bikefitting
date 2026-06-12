package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamJoinApplication;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamJoinApplicationRepository;
import com.starrysky.aries.peloton.schema.CreateTeamDTO;
import com.starrysky.aries.peloton.schema.PelotonPageResponse;
import com.starrysky.aries.peloton.schema.PelotonTeamMemberVO;
import com.starrysky.aries.peloton.schema.PelotonTeamQueryDTO;
import com.starrysky.aries.peloton.schema.PelotonTeamVO;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PelotonTeamService
 * 车队业务服务层
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Service
public class PelotonTeamService {

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private TeamJoinApplicationRepository applicationRepository;

    @Resource
    private UserRepository userRepository;

    /**
     * 获取车队列表（支持分类筛选、关键词搜索、分页）
     *
     * @param queryDTO 查询参数
     * @return 分页车队列表
     */
    public PelotonPageResponse<PelotonTeamVO> getTeamList(PelotonTeamQueryDTO queryDTO) {
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : PelotonTeamQueryDTO.DEFAULT_PAGE;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : PelotonTeamQueryDTO.DEFAULT_SIZE;
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);

        String keyword = StringUtils.hasText(queryDTO.getKeyword()) ? queryDTO.getKeyword().trim() : null;
        String category = StringUtils.hasText(queryDTO.getCategory()) ? queryDTO.getCategory().trim() : null;

        Page<PelotonTeam> teamPage;

        if (StringUtils.hasText(category) && StringUtils.hasText(keyword)) {
            // 分类 + 关键词组合查询
            teamPage = pelotonTeamRepository.searchByActivityLevelAndKeyword(category, keyword, pageRequest);
        } else if (StringUtils.hasText(category)) {
            // 仅分类筛选
            teamPage = pelotonTeamRepository.findByActivityLevel(category, pageRequest);
        } else if (StringUtils.hasText(keyword)) {
            // 仅关键词搜索
            teamPage = pelotonTeamRepository.searchByKeyword(keyword, pageRequest);
        } else {
            // 默认查询全部（按创建时间倒序）
            teamPage = pelotonTeamRepository.findAllByOrderByCreateTimeDesc(pageRequest);
        }

        List<PelotonTeamVO> records = teamPage.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PelotonPageResponse.<PelotonTeamVO>builder()
                .records(records)
                .total(teamPage.getTotalElements())
                .page(page)
                .size(size)
                .pages(teamPage.getTotalPages())
                .build();
    }

    /**
     * 创建车队
     * <p>
     * 创建车队后自动将创建者添加为队长，初始成员数为1。
     *
     * @param dto  创建车队参数
     * @param user 当前登录用户
     * @return 创建的车队信息
     */
    @Transactional(rollbackFor = Exception.class)
    public PelotonTeamVO createTeam(CreateTeamDTO dto, User user) {
        // 1. 校验车队名称唯一性
        if (pelotonTeamRepository.existsByName(dto.getName().trim())) {
            throw new BizException(400, "车队名称已被使用，请换一个名称");
        }

        // 2. 组装车队实体
        String tagsStr = (dto.getTags() != null && !dto.getTags().isEmpty())
                ? String.join(",", dto.getTags())
                : null;

        PelotonTeam team = PelotonTeam.builder()
                .name(dto.getName().trim())
                .avatar(dto.getAvatar())
                .coverImage(dto.getCoverImage())
                .location(dto.getLocation() != null ? dto.getLocation().trim() : null)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .tags(tagsStr)
                .activityLevel("new")
                .memberCount(1)
                .creatorId(user.getId())
                .build();
        team = pelotonTeamRepository.save(team);

        // 3. 将创建者添加为队长
        TeamMember captain = TeamMember.builder()
                .teamId(team.getId())
                .userId(user.getId())
                .role(TeamMember.ROLE_CAPTAIN)
                .tag("新秀")
                .joinDate(LocalDate.now())
                .status(TeamMember.STATUS_ACTIVE)
                .build();
        teamMemberRepository.save(captain);

        return convertToVO(team);
    }

    /**
     * 编辑车队信息
     * <p>
     * 仅车队创建者可操作。修改车队名称时会校验唯一性。
     *
     * @param teamId 车队ID
     * @param dto    编辑内容（与创建时相同结构）
     * @param user   当前登录用户
     * @return 更新后的车队信息
     */
    @Transactional(rollbackFor = Exception.class)
    public PelotonTeamVO updateTeam(Long teamId, CreateTeamDTO dto, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, user, "仅车队创建者可编辑车队信息");

        // 名称唯一性校验（排除自身）
        String newName = dto.getName().trim();
        if (!newName.equals(team.getName()) && pelotonTeamRepository.existsByName(newName)) {
            throw new BizException(400, "车队名称已被使用，请换一个名称");
        }

        team.setName(newName);
        team.setAvatar(dto.getAvatar());
        team.setCoverImage(dto.getCoverImage());
        team.setLocation(dto.getLocation() != null ? dto.getLocation().trim() : null);
        team.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        team.setTags((dto.getTags() != null && !dto.getTags().isEmpty())
                ? String.join(",", dto.getTags()) : null);

        pelotonTeamRepository.save(team);
        return convertToVO(team);
    }

    /**
     * 获取我的车队（我创建的 + 我加入的，去重）
     *
     * @param user 当前登录用户
     * @return 我的车队列表
     */
    public List<PelotonTeamVO> getMyTeams(User user) {
        return pelotonTeamRepository.findTeamsByUserId(user.getId()).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取车队详情
     *
     * @param teamId 车队ID
     * @return 车队详情
     */
    public PelotonTeamVO getTeamDetail(Long teamId) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        return convertToVO(team);
    }

    /**
     * 获取车队成员列表
     *
     * @param teamId 车队ID
     * @return 成员列表（含用户信息）
     */
    public List<PelotonTeamMemberVO> getTeamMembers(Long teamId) {
        pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        List<TeamMember> members = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE);
        Map<Long, User> userMap = buildUserMap(members, TeamMember::getUserId);

        return members.stream()
                .map(m -> buildMemberVO(m, userMap.get(m.getUserId())))
                .collect(Collectors.toList());
    }

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
        assertTeamAdmin(team, user);

        List<TeamJoinApplication> applications = applicationRepository
                .findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, TeamJoinApplication.STATUS_PENDING);
        Map<Long, User> userMap = buildUserMap(applications, TeamJoinApplication::getApplicantId);

        return applications.stream()
                .map(a -> buildApplicationVO(a, userMap.get(a.getApplicantId())))
                .collect(Collectors.toList());
    }

    /**
     * 退出车队
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void leaveTeam(Long teamId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        TeamMember member = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(400, "您尚未加入该车队"));

        if (member.getRole() == TeamMember.ROLE_CAPTAIN) {
            throw new BizException(400, "队长不能退出车队，请先转让队长身份");
        }

        member.setStatus(TeamMember.STATUS_LEFT);
        teamMemberRepository.save(member);

        decrementMemberCount(team);
    }

    /**
     * 队长退出车队（需指定新队长）
     * <p>
     * 仅车队创建者可操作。将队长身份转让给指定成员后，原队长退出车队。
     *
     * @param teamId          车队ID
     * @param newCaptainUserId 新队长的用户ID
     * @param user            当前登录用户（原队长）
     */
    @Transactional(rollbackFor = Exception.class)
    public void captainLeaveTeam(Long teamId, Long newCaptainUserId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, user, "仅车队创建者可执行此操作");

        // 不能把队长身份转给自己
        if (Objects.equals(user.getId(), newCaptainUserId)) {
            throw new BizException(400, "不能将队长身份转让给自己");
        }

        // 查找新队长的活跃成员记录
        TeamMember newCaptainMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, newCaptainUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(400, "指定的新队长不是当前车队成员"));

        // 查找当前队长的成员记录
        TeamMember currentCaptainMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(400, "您尚未加入该车队"));

        // 转让队长身份
        newCaptainMember.setRole(TeamMember.ROLE_CAPTAIN);
        teamMemberRepository.save(newCaptainMember);

        // 原队长退出车队
        currentCaptainMember.setStatus(TeamMember.STATUS_LEFT);
        teamMemberRepository.save(currentCaptainMember);

        // 更新车队创建者ID为新队长
        team.setCreatorId(newCaptainUserId);
        decrementMemberCount(team);
    }

    /**
     * 解散车队
     * <p>
     * 仅车队创建者可操作。删除所有成员记录后删除车队。
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void disbandTeam(Long teamId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, user, "仅车队创建者可解散车队");

        applicationRepository.deleteByTeamId(teamId);
        // 将所有活跃成员标记为已退出（软删除）
        teamMemberRepository.updateStatusByTeamIdAndStatus(teamId, TeamMember.STATUS_ACTIVE, TeamMember.STATUS_LEFT);
        pelotonTeamRepository.delete(team);
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

        // 校验申请理由非空
        String trimmedReason = reason != null ? reason.trim() : "";
        if (!StringUtils.hasText(trimmedReason)) {
            throw new BizException(400, "请填写申请理由");
        }

        // 检查是否已有待审核申请
        applicationRepository.findByTeamIdAndApplicantIdAndStatus(teamId, user.getId(), TeamJoinApplication.STATUS_PENDING)
                .ifPresent(a -> {
                    throw new BizException(400, "您已提交过申请，请等待审核");
                });

        // 检查是否已是成员
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
        assertTeamAdmin(team, admin);

        // 二次校验：申请人是否已通过其他途径成为成员
        teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), application.getApplicantId(), TeamMember.STATUS_ACTIVE)
                .ifPresent(m -> {
                    application.setStatus(TeamJoinApplication.STATUS_APPROVED);
                    application.setReviewerId(admin.getId());
                    applicationRepository.save(application);
                    throw new BizException(400, "该用户已是车队成员，申请已自动通过");
                });

        // 更新申请状态为通过
        application.setStatus(TeamJoinApplication.STATUS_APPROVED);
        application.setReviewerId(admin.getId());
        applicationRepository.save(application);

        // 创建成员记录
        TeamMember member = TeamMember.builder()
                .teamId(application.getTeamId())
                .userId(application.getApplicantId())
                .role(TeamMember.ROLE_MEMBER)
                .tag("新秀")
                .joinDate(LocalDate.now())
                .status(TeamMember.STATUS_ACTIVE)
                .build();
        teamMemberRepository.save(member);

        incrementMemberCount(team);
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
        assertTeamAdmin(team, admin);

        application.setStatus(TeamJoinApplication.STATUS_REJECTED);
        application.setReviewerId(admin.getId());
        applicationRepository.save(application);
    }

    // ============================ 私有方法 ============================

    /**
     * 校验当前用户是否为车队创建者，不是则抛出 403 异常
     */
    private void assertCreator(PelotonTeam team, User user, String message) {
        if (!Objects.equals(team.getCreatorId(), user.getId())) {
            throw new BizException(403, message);
        }
    }

    /**
     * 校验当前用户是否为车队管理员（队长或管理员）
     */
    private void assertTeamAdmin(PelotonTeam team, User user) {
        if (Objects.equals(team.getCreatorId(), user.getId())) {
            return;
        }
        teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMember.STATUS_ACTIVE)
                .filter(m -> m.getRole() <= TeamMember.ROLE_ADMIN)
                .orElseThrow(() -> new BizException(403, "仅队长或管理员可执行此操作"));
    }

    /**
     * 设为管理员（仅队长可操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(Long teamId, Long targetUserId, User admin) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, admin, "仅队长可设为管理员");

        TeamMember member = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, targetUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "该成员不存在"));
        if (member.getRole() == TeamMember.ROLE_CAPTAIN) {
            throw new BizException(400, "不能修改队长的角色");
        }
        if (member.getRole() == TeamMember.ROLE_ADMIN) {
            throw new BizException(400, "该成员已是管理员");
        }
        member.setRole(TeamMember.ROLE_ADMIN);
        teamMemberRepository.save(member);
    }

    /**
     * 撤销管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeAdmin(Long teamId, Long targetUserId, User admin) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        // 仅队长可撤销管理员
        assertCreator(team, admin, "仅队长可撤销管理员");

        TeamMember member = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, targetUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "该成员不存在"));
        if (member.getRole() != TeamMember.ROLE_ADMIN) {
            throw new BizException(400, "该成员不是管理员");
        }
        member.setRole(TeamMember.ROLE_MEMBER);
        teamMemberRepository.save(member);
    }

    /**
     * 请离车队（仅队长可操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Long teamId, Long targetUserId, User admin) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, admin, "仅队长可请离成员");

        TeamMember member = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, targetUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "该成员不存在"));
        if (member.getRole() == TeamMember.ROLE_CAPTAIN) {
            throw new BizException(400, "不能请离队长");
        }

        member.setStatus(TeamMember.STATUS_KICKED);
        teamMemberRepository.save(member);

        decrementMemberCount(team);
    }

    /**
     * 修改成员骑行标签
     * <ul>
     *   <li>队长：可修改所有人（包括自己）</li>
     *   <li>管理员：可修改除队长外所有人</li>
     *   <li>普通成员：仅可修改自己</li>
     * </ul>
     *
     * @param teamId       车队ID
     * @param targetUserId 目标用户ID
     * @param tag          新标签
     * @param user         当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberTag(Long teamId, Long targetUserId, String tag, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        if (!StringUtils.hasText(tag) || tag.trim().length() > 20) {
            throw new BizException(400, "标签无效");
        }

        TeamMember targetMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, targetUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "该成员不存在"));

        // 判断操作人身份
        boolean isCreator = Objects.equals(team.getCreatorId(), user.getId());
        boolean isAdmin = false;
        boolean isSelf = Objects.equals(user.getId(), targetUserId);

        if (!isCreator) {
            TeamMember operatorMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                    .orElseThrow(() -> new BizException(403, "非车队成员无权操作"));
            isAdmin = operatorMember.getRole() == TeamMember.ROLE_ADMIN;
        }

        // 权限校验
        if (isCreator) {
            // 队长：可修改所有人（包括自己），无限制
        } else if (isAdmin) {
            // 管理员：不能修改队长的标签
            if (targetMember.getRole() == TeamMember.ROLE_CAPTAIN) {
                throw new BizException(403, "管理员不能修改队长的标签");
            }
        } else {
            // 普通成员：仅可修改自己
            if (!isSelf) {
                throw new BizException(403, "仅可修改自己的标签");
            }
        }

        targetMember.setTag(tag.trim());
        teamMemberRepository.save(targetMember);
    }

    /**
     * 车队实体 -> VO
     */
    private PelotonTeamVO convertToVO(PelotonTeam team) {
        List<String> tagList = StringUtils.hasText(team.getTags())
                ? Arrays.asList(team.getTags().split(","))
                : Collections.emptyList();

        return PelotonTeamVO.builder()
                .id(team.getId())
                .creatorId(team.getCreatorId())
                .name(team.getName())
                .avatar(team.getAvatar())
                .coverImage(team.getCoverImage())
                .location(team.getLocation())
                .description(team.getDescription())
                .memberCount(team.getMemberCount())
                .activityLevel(team.getActivityLevel())
                .tags(tagList)
                .createdAt(team.getCreateTime())
                .build();
    }

    /**
     * 车队成员实体 -> VO
     */
    private PelotonTeamMemberVO buildMemberVO(TeamMember member, User user) {
        PelotonTeamMemberVO.PelotonTeamMemberVOBuilder builder = PelotonTeamMemberVO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .role(convertRole(member.getRole()))
                .tag(member.getTag())
                .joinDate(member.getJoinDate() != null ? member.getJoinDate().atStartOfDay() : null);

        if (user != null) {
            builder.name(user.getName());
            builder.avatar(user.getAvatar());
        }

        return builder.build();
    }

    /**
     * 角色整数 -> 中文字符串
     */
    private String convertRole(Integer role) {
        if (role == null) return "成员";
        return switch (role) {
            case TeamMember.ROLE_CAPTAIN -> "队长";
            case TeamMember.ROLE_ADMIN -> "管理员";
            default -> "成员";
        };
    }

    /**
     * 批量关联用户信息：从实体列表中提取 userId 并查询 User，返回 userId -> User 映射
     */
    private <T> Map<Long, User> buildUserMap(List<T> entities, Function<T, Long> userIdExtractor) {
        Set<Long> userIds = entities.stream()
                .map(userIdExtractor)
                .collect(Collectors.toSet());
        return userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * 车队成员数 +1 并持久化
     */
    private void incrementMemberCount(PelotonTeam team) {
        team.setMemberCount(team.getMemberCount() + 1);
        pelotonTeamRepository.save(team);
    }

    /**
     * 车队成员数 -1（不低于0）并持久化
     */
    private void decrementMemberCount(PelotonTeam team) {
        team.setMemberCount(Math.max(0, team.getMemberCount() - 1));
        pelotonTeamRepository.save(team);
    }

    /**
     * 申请记录 -> VO（复用 MemberVO 结构）
     */
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
