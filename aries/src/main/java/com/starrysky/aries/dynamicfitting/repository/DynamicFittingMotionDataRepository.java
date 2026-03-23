package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingMotionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DynamicFittingMotionDataRepository extends JpaRepository<DynamicFittingMotionData, Long> {
    Optional<DynamicFittingMotionData> findBySession_IdAndViewType(Long sessionId, String viewType);

    long countBySession_Id(Long sessionId);
}
