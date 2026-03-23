package com.starrysky.aries.bikemodel.repository;

import com.starrysky.aries.entity.BikeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BikeModelRepository extends JpaRepository<BikeModel, Long> {
    Optional<BikeModel> findByBrandAndModelAndModelYearAndSize(String brand, String model, String modelYear,
            String size);

    /**
     * 模糊搜索品牌和型号名 (去重列表)
     */
    @Query("SELECT DISTINCT b.brand, b.brandCn, b.model FROM BikeModel b " +
            "WHERE b.brand LIKE %:keyword% " +
            "OR b.brandCn LIKE %:keyword% " +
            "OR b.model LIKE %:keyword%")
    List<Object[]> findDistinctBrandAndModelByKeyword(@Param("keyword") String keyword);

    /**
     * 获取选定型号的所有变体
     */
    List<BikeModel> findByBrandAndModel(String brand, String model);
}
