package com.starrysky.aries.membership.service;

import com.starrysky.aries.entity.UserMembership;
import com.starrysky.aries.membership.enums.MemberTypeEnum;
import com.starrysky.aries.membership.enums.PrivilegeTypeEnum;
import com.starrysky.aries.membership.exception.MembershipException;
import com.starrysky.aries.membership.repository.UserMembershipRepository;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 会员相关业务类
 *
 * @author adi
 * @version 1.0
 * @since 2026-5-6
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMembershipService {

    private final UserMembershipRepository userMembershipRepository;
    private final StringRedisTemplate stringRedisTemplate;
    // 使用UserBikeRepository来检查车辆数量
    private final UserBikeRepository userBikeRepository;

    // 会员每日也仅限10次动态Fittig，防止一个用户多次替他人fitting
    private static final int PRO_DAILY_DYNAMIC_LIMIT = 10;
    private static final int FREE_BIKE_LIMIT = 2;
    private static final int PRO_BIKE_LIMIT = 10;

    /**
     * 验证和消费特权
     *
     * @param userId        用户ID
     * @param privilegeType 特权类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void validateAndConsumePrivilege(Long userId, PrivilegeTypeEnum privilegeType) {
        // 懒加载或初始化用户的账户状态
        UserMembership membership = userMembershipRepository.findById(userId)
                .orElseGet(() -> initNewMembership(userId));

        switch (privilegeType) {
            case DYNAMIC_FITTING:
//                handleDynamicFittingConsumption(membership);
                break;
            case GARAGE_EXPANSION:
                handleGarageExpansionCheck(membership);
                break;
            case REALTIME_MONITOR:
                handleRealtimeMonitorCheck(membership);
                break;
            case HISTORY_SNAPSHOT:
                handleHistorySnapshotCheck(membership);
                break;
            default:
                throw new MembershipException("未知的特权类型: " + privilegeType);
        }
    }

    /**
     * 处理动态Fitting消费
     *
     * @param membership 用户会员信息
     */
//    private void handleDynamicFittingConsumption(UserMembership membership) {
//        boolean isPro = membership.isProActive();
//
//        if (isPro) {
//            // PRO用户：检查当日限额 10 次
//            String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
////            String redisKey = RedisConstant.MEM_DYNAMIC_QUOTA + dateKey + ":" + membership.getUserId();
//
//            long currentUsage = stringRedisTemplate.opsForValue().increment(redisKey);
//            if (currentUsage == 1) {
//                // 第一次访问设置过期时间为24小时
//                stringRedisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
//            }
//
//            if (currentUsage > PRO_DAILY_DYNAMIC_LIMIT) {
//                log.warn("User {} exceeded PRO daily dynamic limit", membership.getUserId());
//                throw new MembershipException("PRO会员每日分析次数已达上限(" + PRO_DAILY_DYNAMIC_LIMIT + "次)");
//            }
//            // 正常放行
//        } else {
//            // 免费用户：惰性扣费逻辑
//            LocalDate today = LocalDate.now();
//            LocalDate lastRefresh = membership.getLastTokenRefreshDate();
//
//            boolean isFreeAvailable = (lastRefresh == null || ChronoUnit.DAYS.between(lastRefresh, today) >= 2);
//
//            if (isFreeAvailable) {
//                // 扣除免费额度，更新最后一次发放/消费日期为今天
//                membership.setLastTokenRefreshDate(today);
//                userMembershipRepository.save(membership);
//                log.info("User {} consumed 2-day free token", membership.getUserId());
//            } else if (membership.getTokenBalance() > 0) {
//                // 扣除永久余额
//                membership.setTokenBalance(membership.getTokenBalance() - 1);
//                userMembershipRepository.save(membership);
//                log.info("User {} consumed 1 permanent token, remaining: {}", membership.getUserId(), membership.getTokenBalance());
//            } else {
//                throw new MembershipException("您的可用分析次数不足，请开通PRO会员或邀请好友获取奖励！");
//            }
//        }
//    }

    /**
     * 处理车库扩展检查
     *
     * @param membership 用户会员信息
     */
    private void handleGarageExpansionCheck(UserMembership membership) {
        long bikeCount = userBikeRepository.countByUserIdAndStatus(membership.getUserId(), 1);

        if (bikeCount >= PRO_BIKE_LIMIT) {
            throw new MembershipException("无论哪种会员级别，最多只能添加 " + PRO_BIKE_LIMIT + " 辆单车");
        }

        if (!membership.isProActive() && bikeCount >= FREE_BIKE_LIMIT) {
            throw new MembershipException("免费用户最多只能添加 " + FREE_BIKE_LIMIT + " 辆单车，请开通PRO会员解锁更大车库！");
        }
    }

    /**
     * 处理AI实时监护检查
     *
     * @param membership 用户会员信息
     */
    private void handleRealtimeMonitorCheck(UserMembership membership) {
        if (!membership.isProActive()) {
            throw new MembershipException("AI实时监护为PRO会员专属特权，请开通后使用！");
        }
        // 注意：单端登录限制在此阶段暂时略过
    }

    /**
     * 处理历史快照对比检查
     *
     * @param membership 用户会员信息
     */
    private void handleHistorySnapshotCheck(UserMembership membership) {
        if (!membership.isProActive()) {
            throw new MembershipException("历史快照对比为PRO会员专属特权，请开通后使用！");
        }
    }

    private UserMembership initNewMembership(Long userId) {
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setMemberType(MemberTypeEnum.FREE);
        membership.setTokenBalance(0);
        membership.setTotalInvites(0);
        return userMembershipRepository.save(membership);
    }
}
