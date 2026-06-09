package com.starrysky.aries.membership.repository;

import com.starrysky.aries.entity.UserInvitationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户邀请注册记录 Repository
 *
 * @author adi
 * @version 1.0
 * @since 2025-12-23
 */
@Repository
public interface UserInvitationLogRepository extends JpaRepository<UserInvitationLog, Long>, JpaSpecificationExecutor<UserInvitationLog> {

    /**
     * 根据被邀请人ID查询邀请记录
     *
     * @param inviteeId 被邀请人ID
     * @return 邀请记录
     */
    UserInvitationLog findByInviteeId(Long inviteeId);
}
