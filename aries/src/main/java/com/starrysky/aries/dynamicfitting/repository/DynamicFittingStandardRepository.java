package com.starrysky.aries.dynamicfitting.repository;

import com.starrysky.aries.entity.DynamicFittingStandard;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动态 Fitting 标准仓库
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
