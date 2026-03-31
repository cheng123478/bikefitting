package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DynamicFittingSessionRepository extends JpaRepository<DynamicFittingSession, Long> {

        @EntityGraph(attributePaths = { "userBike", "userBike.bikeModel", "userBody" })
        Optional<DynamicFittingSession> findById(Long id);

        /**
         * 使用 EntityGraph 一次性抓取关联数据，防止 N+1
         * 加入 IsDeleted 条件
         */
        @EntityGraph(attributePaths = { "userBike", "userBody" })
        Page<DynamicFittingSession> findByUser_IdAndIsDeleted(Long userId, Integer isDeleted, Pageable pageable);

        @EntityGraph(attributePaths = { "userBike", "userBody" })
        Page<DynamicFittingSession> findByUser_IdAndUserBike_IdAndIsDeleted(Long userId, Long bikeId,
                        Integer isDeleted, Pageable pageable);
}
