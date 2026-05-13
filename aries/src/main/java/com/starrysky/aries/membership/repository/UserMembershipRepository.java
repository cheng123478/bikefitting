package com.starrysky.aries.membership.repository;

import com.starrysky.aries.entity.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户会员特权与资产账户 Repository
 *
 * @author adi
 * @since 2025-12-23
 * @version 1.0
 */
@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long>, JpaSpecificationExecutor<UserMembership> {
}
