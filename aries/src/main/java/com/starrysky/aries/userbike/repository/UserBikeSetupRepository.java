package com.starrysky.aries.userbike.repository;

import com.starrysky.aries.entity.UserBikeSetup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBikeSetupRepository extends JpaRepository<UserBikeSetup, Long> {
    List<UserBikeSetup> findByUserBikeIdOrderByCreateTimeDesc(Long userBikeId);

    Optional<UserBikeSetup> findTopByUserBikeIdOrderByCreateTimeDesc(Long userBikeId);
}
