package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface DynamicFittingResultRepository extends JpaRepository<DynamicFittingResult, Long> {

    @Query("SELECT s.userBike.id FROM DynamicFittingSession s JOIN DynamicFittingResult r ON s.id = r.session.id WHERE s.user.id = :userId")
    Set<Long> findBikeIdsByUserId(@Param("userId") Long userId);

    Optional<DynamicFittingResult> findBySession_Id(Long sessionId);
}
