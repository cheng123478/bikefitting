package com.starrysky.aries.bikemodel.repository;

import com.starrysky.aries.entity.BikeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BikeModelRepository extends JpaRepository<BikeModel, Long> {
    /**
     * 根据品牌、型号、年份、尺寸查询
     *
     * @param brand     品牌
     * @param model     型号
     * @param modelYear 年份
     * @param size      尺寸
     * @return 查询结果
     */
    Optional<BikeModel> findByBrandAndModelAndModelYearAndSize(String brand, String model, String modelYear,
                                                               String size);

    /**
     * 模糊搜索品牌和型号名
     *
     * @param keyword 关键字
     * @return 搜索结果
     */
    @Query("SELECT DISTINCT b.brand, b.brandCn, b.model FROM BikeModel b " +
            "WHERE b.brand LIKE %:keyword% " +
            "OR b.brandCn LIKE %:keyword% " +
            "OR b.model LIKE %:keyword%")
    List<Object[]> findDistinctBrandAndModelByKeyword(@Param("keyword") String keyword);

    /**
     * 获取选定型号的所有变体
     *
     * @param brand 品牌
     * @param model 型号
     * @return 查询结果
     */
    List<BikeModel> findByBrandAndModel(String brand, String model);
}
