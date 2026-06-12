package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.PelotonTeamMember;
import com.starrysky.aries.peloton.repository.PelotonTeamMemberRepository;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.StringUtils;

import java.util.*;
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
    private PelotonTeamMemberRepository pelotonTeamMemberRepository;

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
        PelotonTeamMember captain = PelotonTeamMember.builder()
                .teamId(team.getId())
                .userId(user.getId())
                .role("队长")
                .build();
        pelotonTeamMemberRepository.save(captain);

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
        // 校验车队是否存在
        pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        List<PelotonTeamMember> members = pelotonTeamMemberRepository.findByTeamIdOrderByJoinDateAsc(teamId);

        // 批量查询所有成员的用户信息（避免 N+1）
        Set<Long> userIds = members.stream()
                .map(PelotonTeamMember::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream()
                .map(m -> buildMemberVO(m, userMap.get(m.getUserId())))
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

        PelotonTeamMember member = pelotonTeamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new BizException(400, "您尚未加入该车队"));

        if ("队长".equals(member.getRole())) {
            throw new BizException(400, "队长不能退出车队，请先转让队长身份");
        }

        pelotonTeamMemberRepository.delete(member);

        team.setMemberCount(Math.max(0, team.getMemberCount() - 1));
        pelotonTeamRepository.save(team);
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

        pelotonTeamMemberRepository.deleteByTeamId(teamId);
        pelotonTeamRepository.delete(team);
    }

    /**
     * 申请加入车队
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void joinTeam(Long teamId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        PelotonTeamMember member = PelotonTeamMember.builder()
                .teamId(teamId)
                .userId(user.getId())
                .role("成员")
                .build();
        try {
            pelotonTeamMemberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new BizException(400, "您已加入该车队，请勿重复申请");
        }

        // 更新车队成员数量（基于当前值 +1，避免并发下 count 读到旧值）
        team.setMemberCount(team.getMemberCount() + 1);
        pelotonTeamRepository.save(team);
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
     * 车队成员实体 -> VO（使用预加载的用户信息）
     */
    private PelotonTeamMemberVO buildMemberVO(PelotonTeamMember member, User user) {
        PelotonTeamMemberVO.PelotonTeamMemberVOBuilder builder = PelotonTeamMemberVO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .role(member.getRole())
                .tag(member.getTag())
                .joinDate(member.getJoinDate());

        if (user != null) {
            builder.name(user.getName());
            builder.avatar(user.getAvatar());
        }

        return builder.build();
    }
}
