package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.notification.enums.NotificationType;
import com.starrysky.aries.notification.service.NotificationService;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.redis.RedisConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TeamMemberService
 * 车队成员管理服务（角色变更、请离、退出、标签修改）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Slf4j
@Service
public class TeamMemberService {

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private NotificationService notificationService;

    /**
     * 退出车队（队长不能退出，需先转让队长身份）
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

        pelotonTeamRepository.decrementMemberCount(teamId);
        evictMemberCaches(teamId, user.getId());

        // 通知队长：成员退出车队
        notificationService.send(team.getCaptainId(),
                NotificationType.MEMBER_LEAVE,
                "成员离开",
                "「" + user.getName() + "」已退出车队",
                teamId);

        log.info("用户 {} 退出车队 {}，当前成员数已原子递减", user.getId(), teamId);
    }

    /**
     * 队长退出车队（需指定新队长）
     * <p>
     * 仅车队创建者可操作。将队长身份转让给指定成员后，原队长退出车队。
     *
     * @param teamId           车队ID
     * @param newCaptainUserId 新队长的用户ID
     * @param user             当前登录用户（原队长）
     */
    @Transactional(rollbackFor = Exception.class)
    public void captainLeaveTeam(Long teamId, Long newCaptainUserId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, user, "仅车队创建者可执行此操作");

        if (Objects.equals(user.getId(), newCaptainUserId)) {
            throw new BizException(400, "不能将队长身份转让给自己");
        }

        TeamMember newCaptainMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, newCaptainUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(400, "指定的新队长不是当前车队成员"));

        TeamMember currentCaptainMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(400, "您尚未加入该车队"));

        newCaptainMember.setRole(TeamMember.ROLE_CAPTAIN);
        teamMemberRepository.save(newCaptainMember);

        currentCaptainMember.setStatus(TeamMember.STATUS_LEFT);
        teamMemberRepository.save(currentCaptainMember);

        team.setCaptainId(newCaptainUserId);
        pelotonTeamRepository.save(team);
        pelotonTeamRepository.decrementMemberCount(teamId);

        // 失效涉及的所有缓存：详情、成员列表、旧队长和新队长的“我的车队”、车队列表
        evictMemberCaches(teamId, user.getId(), newCaptainUserId);
    
        // 通知所有队内成员：队长转让并退出（排除新旧队长）
        List<Long> memberIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinDateAsc(teamId, TeamMember.STATUS_ACTIVE)
                .stream()
                .map(TeamMember::getUserId)
                .filter(uid -> !uid.equals(user.getId()) && !uid.equals(newCaptainUserId))
                .collect(Collectors.toList());
        notificationService.sendBatch(memberIds,
                NotificationType.CAPTAIN_LEAVE,
                "队长转让",
                "「" + (user.getName() != null ? user.getName() : "原队长") + "」已将队长身份转让并退出车队",
                teamId);
    
        // 通知新队长：您已成为新队长
        notificationService.send(newCaptainUserId,
                NotificationType.MEMBER_PROMOTE,
                "队长任命",
                "您已被任命为「" + team.getName() + "」的队长",
                teamId);
    
        log.info("用户 {} 将队长身份转让给 {}（车队 {}），原队长已退出",
                user.getId(), newCaptainUserId, teamId);
    }

    /**
     * 设为管理员（仅队长可操作）
     *
     * @param teamId       车队ID
     * @param targetUserId 目标用户ID
     * @param admin        当前登录用户
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

        // 通知目标用户：被任命为管理员
        notificationService.send(targetUserId,
                NotificationType.MEMBER_PROMOTE,
                "管理员任命",
                "您已被提升为「" + team.getName() + "」管理员",
                teamId);

        log.info("用户 {} 在车队 {} 中将 {} 设为管理员", admin.getId(), teamId, targetUserId);
    }

    /**
     * 撤销管理员（仅队长可操作）
     *
     * @param teamId       车队ID
     * @param targetUserId 目标用户ID
     * @param admin        当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeAdmin(Long teamId, Long targetUserId, User admin) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));
        assertCreator(team, admin, "仅队长可撤销管理员");

        TeamMember member = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, targetUserId, TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "该成员不存在"));
        if (member.getRole() != TeamMember.ROLE_ADMIN) {
            throw new BizException(400, "该成员不是管理员");
        }
        member.setRole(TeamMember.ROLE_MEMBER);
        teamMemberRepository.save(member);

        // 通知目标用户：被撤销管理员
        notificationService.send(targetUserId,
                NotificationType.MEMBER_DEMOTE,
                "管理员撤销",
                "您在「" + team.getName() + "」的管理员权限已被撤销",
                teamId);

        log.info("用户 {} 在车队 {} 中撤销了 {} 的管理员权限", admin.getId(), teamId, targetUserId);
    }

    /**
     * 请离车队（仅队长可操作）
     *
     * @param teamId       车队ID
     * @param targetUserId 目标用户ID
     * @param admin        当前登录用户
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

        pelotonTeamRepository.decrementMemberCount(teamId);
        evictMemberCaches(teamId, targetUserId);

        // 通知被请离的成员
        notificationService.send(targetUserId,
                NotificationType.MEMBER_KICKED,
                "您已被请离车队",
                "您已被「" + team.getName() + "」车队请离",
                teamId);

        log.info("用户 {} 在车队 {} 中请离了成员 {}", admin.getId(), teamId, targetUserId);
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

        boolean isCreator = Objects.equals(team.getCaptainId(), user.getId());
        boolean isSelf = Objects.equals(user.getId(), targetUserId);

        if (!isCreator) {
            TeamMember operatorMember = teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                    .orElseThrow(() -> new BizException(403, "非车队成员无权操作"));
            boolean isAdmin = operatorMember.getRole() == TeamMember.ROLE_ADMIN;

            if (isAdmin && targetMember.getRole() == TeamMember.ROLE_CAPTAIN) {
                throw new BizException(403, "管理员不能修改队长的标签");
            }
            if (!isAdmin && !isSelf) {
                throw new BizException(403, "仅可修改自己的标签");
            }
        }

        String trimmedTag = tag.trim();
        if (trimmedTag.equals(targetMember.getTag())) {
            return;
        }

        targetMember.setTag(trimmedTag);
        teamMemberRepository.save(targetMember);
        log.info("用户 {} 在车队 {} 中将成员 {} 的标签修改为 {}", user.getId(), teamId, targetUserId, trimmedTag);
    }

    // ============================ 内部方法 ============================

    /**
     * 校验当前用户是否为车队队长（使用 captainId 判断）
     */
    void assertCreator(PelotonTeam team, User user, String message) {
        if (!Objects.equals(team.getCaptainId(), user.getId())) {
            throw new BizException(403, message);
        }
    }

    /**
     * 校验当前用户是否为车队管理员（队长或管理员）
     */
    void assertTeamAdmin(PelotonTeam team, User user) {
        if (Objects.equals(team.getCaptainId(), user.getId())) {
            return;
        }
        teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMember.STATUS_ACTIVE)
                .filter(m -> m.getRole() == TeamMember.ROLE_CAPTAIN || m.getRole() == TeamMember.ROLE_ADMIN)
                .orElseThrow(() -> new BizException(403, "仅队长或管理员可执行此操作"));
    }

    /**
     * 校验当前用户是否为车队活跃成员（任意角色均可）
     */
    void assertTeamMember(PelotonTeam team, User user) {
        if (Objects.equals(team.getCaptainId(), user.getId())) {
            return;
        }
        teamMemberRepository.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(403, "仅队内成员可执行此操作"));
    }

    /**
     * 车队成员数 -1（数据库原子操作，避免并发覆盖）
     */
    void decrementMemberCount(Long teamId) {
        pelotonTeamRepository.decrementMemberCount(teamId);
    }

    /**
     * 车队成员数 +1（数据库原子操作，避免并发覆盖）
     */
    void incrementMemberCount(Long teamId) {
        pelotonTeamRepository.incrementMemberCount(teamId);
    }

    /**
     * 成员变动时精确失效相关缓存
     * <ul>
     *   <li>{@code peloton:teams:detail::{teamId}} — 详情页 memberCount 已变</li>
     *   <li>{@code peloton:teams:members::{teamId}:*} — 成员列表已变</li>
     *   <li>{@code peloton:teams:my::{userId}} — 该用户的"我的车队"已变</li>
     * </ul>
     *
     * @param teamId  车队ID
     * @param userIds 受影响的用户ID（退出者、被请离者、新旧队长等）
     */
    private void evictMemberCaches(Long teamId, Long... userIds) {
        if (cacheManager == null) return;
    
        // 失效详情缓存
        Cache detailCache = cacheManager.getCache(RedisConstant.PELOTON_TEAMS_DETAIL);
        if (detailCache != null) {
            detailCache.evict(teamId);
        }
    
        // 失效成员列表缓存（使用 allEntries 因为分页 key 无法精确枚举）
        Cache membersCache = cacheManager.getCache(RedisConstant.PELOTON_TEAMS_MEMBERS);
        if (membersCache != null) {
            membersCache.clear();
        }
    
        // 失效车队列表缓存（成员变动影响列表展示）
        Cache listCache = cacheManager.getCache(RedisConstant.PELOTON_TEAMS_LIST);
        if (listCache != null) {
            listCache.clear();
        }
    
        // 失效受影响用户的“我的车队”缓存
        Cache myCache = cacheManager.getCache(RedisConstant.PELOTON_TEAMS_MY);
        if (myCache != null && userIds != null) {
            for (Long uid : userIds) {
                if (uid != null) {
                    myCache.evict(uid);
                }
            }
        }
    }
}
