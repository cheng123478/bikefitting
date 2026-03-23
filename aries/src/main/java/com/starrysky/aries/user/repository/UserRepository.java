package com.starrysky.aries.user.repository;

import com.starrysky.aries.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByPhone(String phone);

    @Modifying
    @Transactional
    @Query(value = "UPDATE usr_user u SET u.phone = NULL, u.old_phone = u.phone, u.union_id = NULL WHERE u.is_deleted = true AND u.delete_time < :deadline", nativeQuery = true)
    int anonymizeExpiredAccounts(@Param("deadline") LocalDateTime deadline);

    /**
     * 1. 查找符合注销条件的用户ID列表
     * 条件：已标记删除 + 删除时间早于截止时间 + (手机号还没被清空 OR unionId还没被清空)
     */
    @Query("SELECT u.id FROM User u WHERE u.isDeleted = true AND u.deleteTime < :deadline AND (u.phone IS NOT NULL OR u.unionId IS NOT NULL)")
    List<String> findIdsToClean(@Param("deadline") LocalDateTime deadline, Pageable pageable);

    /**
     * 2. 根据ID批量清空手机号和unionId
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.phone = NULL, u.unionId = NULL WHERE u.id IN :ids")
    void cleanPhoneBatchByIds(@Param("ids") List<String> ids);

    /**
     * 3. 根据微信UnionId查找用户
     */
    User findByUnionId(String unionId);
}


