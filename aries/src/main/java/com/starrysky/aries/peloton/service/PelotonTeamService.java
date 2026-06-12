package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.PelotonTeamMember;
import com.starrysky.aries.peloton.repository.PelotonTeamMemberRepository;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.schema.PelotonPageResponse;
import com.starrysky.aries.peloton.schema.PelotonTeamMemberVO;
import com.starrysky.aries.peloton.schema.PelotonTeamQueryDTO;
import com.starrysky.aries.peloton.schema.PelotonTeamVO;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@Slf4j
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
        int page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null ? queryDTO.getSize() : 8;
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
     * 获取我的车队（我创建的 + 我加入的，去重）
     *
     * @param user 当前登录用户
     * @return 我的车队列表
     */
    public List<PelotonTeamVO> getMyTeams(User user) {
        // 1. 查询我创建的车队
        List<PelotonTeam> createdTeams = pelotonTeamRepository.findByCreatorId(user.getId());
        Set<Long> teamIds = createdTeams.stream()
                .map(PelotonTeam::getId)
                .collect(Collectors.toSet());

        // 2. 查询我加入的车队（排除已创建的，避免重复）
        List<PelotonTeamMember> memberships = pelotonTeamMemberRepository.findByUserId(user.getId());
        List<Long> joinedTeamIds = memberships.stream()
                .map(PelotonTeamMember::getTeamId)
                .filter(id -> !teamIds.contains(id))
                .collect(Collectors.toList());

        List<PelotonTeam> joinedTeams = joinedTeamIds.isEmpty()
                ? Collections.emptyList()
                : pelotonTeamRepository.findByIdIn(joinedTeamIds);

        // 3. 合并并转换为VO
        List<PelotonTeam> allTeams = new ArrayList<>(createdTeams);
        allTeams.addAll(joinedTeams);

        return allTeams.stream()
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
        return members.stream()
                .map(this::convertMemberToVO)
                .collect(Collectors.toList());
    }

    /**
     * 申请加入车队
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void joinTeam(Long teamId, User user) {
        // 校验车队是否存在
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        // 校验是否已加入
        pelotonTeamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .ifPresent(member -> {
                    throw new BizException(400, "您已加入该车队，请勿重复申请");
                });

        // 创建成员记录
        PelotonTeamMember member = PelotonTeamMember.builder()
                .teamId(teamId)
                .userId(user.getId())
                .role("成员")
                .build();
        pelotonTeamMemberRepository.save(member);

        // 更新车队成员数量
        long newCount = pelotonTeamMemberRepository.countByTeamId(teamId);
        team.setMemberCount((int) newCount);
        pelotonTeamRepository.save(team);

        log.info("用户 [{}] 加入车队 [{}] 成功", user.getId(), teamId);
    }

    // ============================ 私有转换方法 ============================

    /**
     * 车队实体 -> VO
     */
    private PelotonTeamVO convertToVO(PelotonTeam team) {
        List<String> tagList = StringUtils.hasText(team.getTags())
                ? Arrays.asList(team.getTags().split(","))
                : Collections.emptyList();

        return PelotonTeamVO.builder()
                .id(team.getId())
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
     * 车队成员实体 -> VO（关联查询用户信息）
     */
    private PelotonTeamMemberVO convertMemberToVO(PelotonTeamMember member) {
        PelotonTeamMemberVO.PelotonTeamMemberVOBuilder builder = PelotonTeamMemberVO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .role(member.getRole())
                .tag(member.getTag())
                .joinDate(member.getJoinDate());

        // 关联查询用户昵称和头像
        userRepository.findById(member.getUserId()).ifPresent(user -> {
            builder.name(user.getName());
            builder.avatar(user.getAvatar());
        });

        return builder.build();
    }
}
