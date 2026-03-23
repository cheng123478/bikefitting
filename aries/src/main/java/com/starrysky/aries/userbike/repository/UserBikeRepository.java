package com.starrysky.aries.userbike.repository;

import com.starrysky.aries.entity.UserBike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBikeRepository extends JpaRepository<UserBike, Long> {
    /**
     * 根据用户ID查询用户车辆信息
     *
     * @param userId 用户ID
     * @return 用户车辆信息
     */
    List<UserBike> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

    /**
     * 根据用户ID查询用户最新车辆信息
     *
     * @param userId 用户ID
     * @return 用户最新车辆信息
     */
    Optional<UserBike> findTopByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Integer status);

}
