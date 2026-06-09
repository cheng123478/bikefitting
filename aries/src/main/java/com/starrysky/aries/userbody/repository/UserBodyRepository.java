package com.starrysky.aries.userbody.repository;

import com.starrysky.aries.entity.UserBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBodyRepository extends JpaRepository<UserBody, Long> {
    /**
     * 获取用户最新的身体档案
     * 逻辑：按主键 ID 倒序排列取第一条（由于是自增ID，ID越大代表录入时间越晚）
     *
     * @param userId 用户唯一标识
     * @return 最新的身体信息快照
     */
    Optional<UserBody> findTopByUserIdOrderByIdDesc(Long userId);

    /**
     * 获取用户所有的身体档案历史
     * 逻辑：按录入时间倒序排列，方便前端展示时间轴或进化历程
     *
     * @param userId 用户唯一标识
     * @return 身体信息历史列表
     */
    Page<UserBody> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
}
