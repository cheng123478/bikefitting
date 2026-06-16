package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamGarageRepository;
import com.starrysky.aries.peloton.repository.TeamJoinApplicationRepository;
import com.starrysky.aries.peloton.repository.TeamNoticeRepository;
import com.starrysky.aries.peloton.repository.TeamPostRepository;
import com.starrysky.aries.peloton.repository.TeamPostInteractionRepository;
import com.starrysky.aries.peloton.schema.CreateTeamDTO;
import com.starrysky.aries.peloton.schema.PelotonPageResponse;
import com.starrysky.aries.peloton.schema.PelotonTeamMemberVO;
import com.starrysky.aries.peloton.schema.PelotonTeamQueryDTO;
import com.starrysky.aries.peloton.schema.PelotonTeamVO;
import com.starrysky.aries.peloton.schema.TeamMemberQueryDTO;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.user.repository.UserRepository;
import com.starrysky.aries.util.PelotonTeamUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PelotonTeamService
 * 车队基本信息服务（列表、创建、编辑、详情、成员查询）
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>{@code peloton:teams:list} — 车队列表分页缓存，60s TTL，不主动失效（靠TTL自动刷新）</li>
 *   <li>{@code peloton:teams:detail} — 车队详情缓存，写操作时精确失效</li>
 *   <li>{@code peloton:teams:my} — 我的车队缓存，成员变动时精确失效</li>
 *   <li>{@code peloton:teams:members} — 成员列表分页缓存，成员变动时精确失效</li>
 * </ul>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Slf4j
@Service
public class PelotonTeamService {

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private TeamJoinApplicationRepository applicationRepository;

    @Resource
    private TeamGarageRepository teamGarageRepository;

    @Resource
    private TeamNoticeRepository teamNoticeRepository;

    @Resource
    private TeamPostRepository teamPostRepository;

    @Resource
    private TeamPostInteractionRepository teamPostInteractionRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private TeamMemberService teamMemberService;

    /**
     * 获取车队列表（支持分类筛选、关键词搜索、分页）
     *
     * @param queryDTO 查询参数
     * @return 分页车队列表
     */
    @Cacheable(value = RedisConstant.PELOTON_TEAMS_LIST, key = "#queryDTO.page + ':' + #queryDTO.size + ':' + #queryDTO.keyword + ':' + #queryDTO.category")
    public PelotonPageResponse<PelotonTeamVO> getTeamList(PelotonTeamQueryDTO queryDTO) {
        int page = queryDTO.getPage();
        int size = queryDTO.getSize();
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);

        String keyword = StringUtils.hasText(queryDTO.getKeyword()) ? queryDTO.getKeyword().trim() : null;
        String category = StringUtils.hasText(queryDTO.getCategory()) ? queryDTO.getCategory().trim() : null;

        Page<PelotonTeam> teamPage;

        if (StringUtils.hasText(category) && StringUtils.hasText(keyword)) {
            teamPage = pelotonTeamRepository.searchByActivityLevelAndKeyword(category, keyword, pageRequest);
        } else if (StringUtils.hasText(category)) {
            teamPage = pelotonTeamRepository.findByActivityLevelOrderByCreateTimeDesc(category, pageRequest);
        } else if (StringUtils.hasText(keyword)) {
            teamPage = pelotonTeamRepository.searchByKeyword(keyword, pageRequest);
        } else {
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
    @Caching(evict = {
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_MY, allEntries = true),
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_LIST, allEntries = true)
    })
    public PelotonTeamVO createTeam(CreateTeamDTO dto, User user) {
        String name = HtmlUtils.htmlEscape(dto.getName().trim());
        if (pelotonTeamRepository.existsByName(name)) {
            throw new BizException(400, "车队名称已被使用，请换一个名称");
        }

        String tagsStr = (dto.getTags() != null && !dto.getTags().isEmpty())
                ? dto.getTags().stream().map(HtmlUtils::htmlEscape).collect(Collectors.joining(","))
                : null;

        PelotonTeam team = PelotonTeam.builder()
                .name(name)
                .avatar(dto.getAvatar())
                .coverImage(dto.getCoverImage())
                .location(dto.getLocation() != null ? HtmlUtils.htmlEscape(dto.getLocation().trim()) : null)
                .description(dto.getDescription() != null ? HtmlUtils.htmlEscape(dto.getDescription().trim()) : null)
                .tags(tagsStr)
                .activityLevel("new")
                .memberCount(1)
                .creatorId(user.getId())
                .captainId(user.getId())
                .build();
        team = pelotonTeamRepository.save(team);

        TeamMember captain = TeamMember.builder()
                .teamId(team.getId())
                .userId(user.getId())
                .role(TeamMember.ROLE_CAPTAIN)
                .tag(TeamMember.DEFAULT_TAG)
                .joinDate(LocalDate.now())
                .status(TeamMember.STATUS_ACTIVE)
                .build();
        teamMemberRepository.save(captain);

        log.info("用户 {} 创建了车队 {}（id={}）", user.getId(), name, team.getId());
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
    @Caching(evict = {
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_DETAIL, key = "#teamId"),
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_MY, allEntries = true)
    })
    public PelotonTeamVO updateTeam(Long teamId, CreateTeamDTO dto, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        teamMemberService.assertCreator(team, user, "仅车队创建者可编辑车队信息");

        String newName = HtmlUtils.htmlEscape(dto.getName().trim());
        if (!newName.equals(team.getName()) && pelotonTeamRepository.existsByName(newName)) {
            throw new BizException(400, "车队名称已被使用，请换一个名称");
        }

        team.setName(newName);
        team.setAvatar(dto.getAvatar());
        team.setCoverImage(dto.getCoverImage());
        team.setLocation(dto.getLocation() != null ? HtmlUtils.htmlEscape(dto.getLocation().trim()) : null);
        team.setDescription(dto.getDescription() != null ? HtmlUtils.htmlEscape(dto.getDescription().trim()) : null);
        team.setTags((dto.getTags() != null && !dto.getTags().isEmpty())
                ? dto.getTags().stream().map(HtmlUtils::htmlEscape).collect(Collectors.joining(",")) : null);

        pelotonTeamRepository.save(team);
        log.info("用户 {} 编辑了车队 {}（id={}）", user.getId(), newName, teamId);
        return convertToVO(team);
    }

    /**
     * 获取我的车队（我创建的 + 我加入的，去重）
     *
     * @param user 当前登录用户
     * @return 我的车队列表
     */
    @Cacheable(value = RedisConstant.PELOTON_TEAMS_MY, key = "#user.id")
    public List<PelotonTeamVO> getMyTeams(User user) {
        return pelotonTeamRepository.findTeamsByUserId(user.getId()).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取车队详情
     * <p>
     * 不存在时返回 {@code null} 以便 Spring Cache 缓存空值标记，防止缓存穿透。
     *
     * @param teamId 车队ID
     * @return 车队详情，不存在时返回 null
     */
    @Cacheable(value = RedisConstant.PELOTON_TEAMS_DETAIL, key = "#teamId")
    public PelotonTeamVO getTeamDetail(Long teamId) {
        return pelotonTeamRepository.findById(teamId)
                .map(this::convertToVO)
                .orElse(null);
    }

    /**
     * 获取车队成员列表（支持分页 + 关键词搜索）
     * <p>
     * 关键词模糊匹配成员昵称或骑行标签。
     *
     * @param teamId   车队ID
     * @param queryDTO 查询参数（分页、关键词）
     * @return 成员分页列表
     */
    @Cacheable(value = RedisConstant.PELOTON_TEAMS_MEMBERS, key = "#teamId + ':' + #queryDTO.page + ':' + #queryDTO.size + ':' + #queryDTO.keyword")
    public PelotonPageResponse<PelotonTeamMemberVO> getTeamMembers(Long teamId, TeamMemberQueryDTO queryDTO) {
        pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        int page = queryDTO.getPage();
        int size = queryDTO.getSize();
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);

        String keyword = StringUtils.hasText(queryDTO.getKeyword()) ? queryDTO.getKeyword().trim() : null;

        Page<TeamMember> memberPage;
        if (StringUtils.hasText(keyword)) {
            memberPage = teamMemberRepository.searchByKeyword(teamId, TeamMember.STATUS_ACTIVE, keyword, pageRequest);
        } else {
            memberPage = teamMemberRepository.findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE, pageRequest);
        }

        Map<Long, User> userMap = PelotonTeamUtil.buildUserMap(memberPage.getContent(), TeamMember::getUserId, userRepository);
        List<PelotonTeamMemberVO> records = memberPage.getContent().stream()
                .map(m -> buildMemberVO(m, userMap.get(m.getUserId())))
                .collect(Collectors.toList());

        return PelotonPageResponse.<PelotonTeamMemberVO>builder()
                .records(records)
                .total(memberPage.getTotalElements())
                .page(page)
                .size(size)
                .pages(memberPage.getTotalPages())
                .build();
    }

    /**
     * 解散车队
     * <p>
     * 仅车队创建者可操作。物理删除所有关联数据后删除车队。
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_DETAIL, key = "#teamId"),
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_MY, allEntries = true),
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_MEMBERS, key = "#teamId"),
            @CacheEvict(value = RedisConstant.PELOTON_TEAMS_GARAGE, key = "#teamId")
    })
    public void disbandTeam(Long teamId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        teamMemberService.assertCreator(team, user, "仅车队创建者可解散车队");

        applicationRepository.deleteByTeamId(teamId);
        teamMemberRepository.deleteByTeamId(teamId);
        teamGarageRepository.deleteByTeamId(teamId);
        teamNoticeRepository.deleteByTeamId(teamId);
        teamPostRepository.deleteByTeamId(teamId);
        teamPostInteractionRepository.deleteByTeamId(teamId);
        pelotonTeamRepository.delete(team);
        log.info("用户 {} 解散了车队 {}（id={}）", user.getId(), team.getName(), teamId);
    }

    // ============================ 私有方法 ============================

    /**
     * 车队实体 -> VO
     */
    private PelotonTeamVO convertToVO(PelotonTeam team) {
        List<String> tagList = team.getTagList();

        return PelotonTeamVO.builder()
                .id(team.getId())
                .creatorId(team.getCreatorId())
                .captainId(team.getCaptainId())
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
}
