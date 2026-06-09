package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingStandard;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动态 Fitting标准仓库
 *
 * @author adice
 * @version 1.0
 * @since 2026-02-08 11:19:57
 */
@Repository
public interface DynamicFittingStandardRepository extends JpaRepository<DynamicFittingStandard, Long> {

    /**
     * 根据单车类型查询标准，并开启缓存
     *
     * @param bikeType 单车类型 code
     * @return 标准列表
     */
    // @Cacheable(value = "fittingStandards", key = "#p0", condition = "#p0 != null")
    List<DynamicFittingStandard> findByBikeType(Integer bikeType);
}
