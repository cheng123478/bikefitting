package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicFittingSnapshotRepository extends JpaRepository<DynamicFittingSnapshot, Long> {
    List<DynamicFittingSnapshot> findBySession_Id(Long sessionId);

    List<DynamicFittingSnapshot> findBySession_IdAndViewType(Long sessionId, String viewType);
}
