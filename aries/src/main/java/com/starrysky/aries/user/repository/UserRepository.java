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
    /**
     * 根据手机号查找用户
     *
     * @param phone 手机号
     * @return 用户
     */
    User findByPhone(String phone);

    /**
     * 查找符合注销条件的用户ID列表
     * 条件：已标记删除 + 删除时间早于截止时间 + (手机号还没被清空 OR unionId还没被清空)
     *
     * @param deadline 过期时间
     * @param pageable 分页参数
     * @return 用户ID列表
     */
    @Query("SELECT u.id FROM User u WHERE u.isDeleted = true AND u.deleteTime < :deadline")
    List<String> findIdsToClean(@Param("deadline") LocalDateTime deadline, Pageable pageable);

    /**
     * 根据ID批量清空手机号和unionId
     *
     * @param ids 用户ID列表
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.phone = NULL, u.unionId = NULL WHERE u.id IN :ids")
    void cleanPhoneBatchByIds(@Param("ids") List<String> ids);

    /**
     * 根据微信UnionId查找用户
     *
     * @param unionId 微信UnionId
     * @return 用户
     */
    User findByUnionId(String unionId);
}


