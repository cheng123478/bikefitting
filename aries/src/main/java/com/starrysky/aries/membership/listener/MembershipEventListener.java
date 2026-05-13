package com.starrysky.aries.membership.listener;

import com.starrysky.aries.entity.UserInvitationLog;
import com.starrysky.aries.entity.UserMembership;
import com.starrysky.aries.membership.enums.MemberTypeEnum;
import com.starrysky.aries.membership.event.UserRegisteredEvent;
import com.starrysky.aries.membership.repository.UserInvitationLogRepository;
import com.starrysky.aries.membership.repository.UserMembershipRepository;
import com.starrysky.aries.util.InviteCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 会员相关事件监听器
 *
 * @author adi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipEventListener {

    private final UserMembershipRepository userMembershipRepository;
    private final UserInvitationLogRepository userInvitationLogRepository;

    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        String inviterCode = event.getInviterCode();
        Long newUserId = event.getNewUserId();

        log.info("Received UserRegisteredEvent: newUserId={}, inviterCode={}", newUserId, inviterCode);

        // 1. 解码邀请码获取邀请人ID
        Long inviterId = InviteCodeUtil.decode(inviterCode);
        if (inviterId == null) {
            log.warn("Invalid inviterCode: {}", inviterCode);
            return;
        }

        // 不能自己邀请自己
        if (inviterId.equals(newUserId)) {
            log.warn("Inviter cannot be the same as Invitee");
            return;
        }

        // 2. 检查幂等性：被邀请人是否已经被记录过
        UserInvitationLog existingLog = userInvitationLogRepository.findByInviteeId(newUserId);
        if (existingLog != null) {
            log.warn("Invitee {} has already been invited", newUserId);
            return;
        }

        // 3. 落库邀请记录
        UserInvitationLog invitationLog = new UserInvitationLog();
        invitationLog.setInviterId(inviterId);
        invitationLog.setInviteeId(newUserId);
        invitationLog.setRewardGranted(false);
        userInvitationLogRepository.save(invitationLog);

        // 4. 发放奖励 (1个永久令牌)
        UserMembership inviterMembership = userMembershipRepository.findById(inviterId)
                .orElseGet(() -> initNewMembership(inviterId));

        inviterMembership.setTokenBalance(inviterMembership.getTokenBalance() + 1);
        inviterMembership.setTotalInvites(inviterMembership.getTotalInvites() + 1);

        // 5. 判断7-7规则：每满7人送7天PRO
        if (inviterMembership.getTotalInvites() % 7 == 0) {
            grantProDays(inviterMembership, 7);
            log.info("User {} reached 7 invites, granted 7 days PRO", inviterId);
        }

        userMembershipRepository.save(inviterMembership);

        // 标记记录为已发放
        invitationLog.setRewardGranted(true);
        userInvitationLogRepository.save(invitationLog);
        
        log.info("Invitation reward granted. InviterId={}, Tokens={}, TotalInvites={}", 
                inviterId, inviterMembership.getTokenBalance(), inviterMembership.getTotalInvites());
    }

    private UserMembership initNewMembership(Long userId) {
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setMemberType(MemberTypeEnum.FREE);
        membership.setTokenBalance(0);
        membership.setTotalInvites(0);
        return membership;
    }

    private void grantProDays(UserMembership membership, int days) {
        LocalDateTime now = LocalDateTime.now();
        if (membership.getMemberType() == MemberTypeEnum.FREE || 
            membership.getProExpireTime() == null || 
            membership.getProExpireTime().isBefore(now)) {
            
            // 从免费升级为PRO，或已过期
            membership.setMemberType(MemberTypeEnum.PRO);
            membership.setProExpireTime(now.plusDays(days));
        } else {
            // PRO尚未过期，追加天数
            membership.setProExpireTime(membership.getProExpireTime().plusDays(days));
        }
    }
}
