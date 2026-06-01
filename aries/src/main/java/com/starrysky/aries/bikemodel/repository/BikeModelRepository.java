package com.starrysky.aries.bikemodel.repository;

import com.starrysky.aries.entity.BikeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BikeModelRepository extends JpaRepository<BikeModel, Long>, JpaSpecificationExecutor<BikeModel> {
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

    /**
     * 分页查询展厅单车列表
     *
     * @param bikeType 车型分类
     * @param pageable 分页参数
     * @return 分页数据
     */
    Page<BikeModel> findByBikeType(Integer bikeType, Pageable pageable);

    /**
     * 根据车型名称查询所有尺寸变体
     *
     * @param model 车型名称
     * @return 该车型的所有尺寸列表
     */
    List<BikeModel> findByModelOrderBySize(String model);

    /**
     * 根据车型名称和品牌查询所有尺寸变体
     *
     * @param model 车型名称
     * @param brand 品牌
     * @return 该车型的所有尺寸列表
     */
    List<BikeModel> findByModelAndBrandOrderBySize(String model, String brand);
    
    /**
     * 根据车型名称获取代表尺寸（最小id的记录）
     * 用于车型统计时的唯一标识
     *
     * @param model 车型名称
     * @return 代表尺寸的车型记录
     */
    @Query("SELECT b FROM BikeModel b WHERE b.model = :model ORDER BY b.id ASC LIMIT 1")
    Optional<BikeModel> findRepresentativeByModel(@Param("model") String model);

    /**
     * 获取所有不重复的品牌列表
     *
     * @return 品牌列表
     */
    @Query("SELECT DISTINCT b.brandCn FROM BikeModel b ORDER BY b.brandCn")
    List<String> findDistinctBrands();

    /**
     * 计算去重后的车型总数
     *
     * @param keyword 搜索关键字（可为空）
     * @param bikeTypes 车型分类列表（可为空）
     * @param brands 品牌列表（可为空）
     * @param minPrice 最低价格（可为空）
     * @param maxPrice 最高价格（可为空）
     * @return 去重后的车型数量
     */
    @Query("SELECT COUNT(DISTINCT b.model) FROM BikeModel b WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR b.brand LIKE %:keyword% OR b.brandCn LIKE %:keyword% OR b.model LIKE %:keyword%) AND " +
            "(:bikeTypes IS NULL OR :bikeTypes = '' OR b.bikeType IN :bikeTypesList) AND " +
            "(:brands IS NULL OR :brands = '' OR b.brandCn IN :brandsList) AND " +
            "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR b.price <= :maxPrice)")
    Long countDistinctModels(
            @Param("keyword") String keyword,
            @Param("bikeTypes") String bikeTypes,
            @Param("bikeTypesList") List<Integer> bikeTypesList,
            @Param("brands") String brands,
            @Param("brandsList") List<String> brandsList,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice);

    /**
     * 查询去重后的车型列表（每个model只返回一条记录）
     *
     * @param keyword 搜索关键字（可为空）
     * @param bikeTypes 车型分类（可为空或逗号分隔）
     * @param bikeTypesList 车型分类列表
     * @param brands 品牌（可为空或逗号分隔）
     * @param brandsList 品牌列表
     * @param minPrice 最低价格（可为空）
     * @param maxPrice 最高价格（可为空）
     * @param pageable 分页参数
     * @return 去重后的车型分页数据
     */
    @Query(value = "SELECT b.* FROM bike_model b WHERE b.id IN (" +
            "SELECT MIN(id) FROM bike_model WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR brand LIKE CONCAT('%', :keyword, '%') OR brand_cn LIKE CONCAT('%', :keyword, '%') OR model LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:bikeTypes IS NULL OR :bikeTypes = '' OR bike_type IN (:bikeTypesList)) AND " +
            "(:brands IS NULL OR :brands = '' OR brand_cn IN (:brandsList)) AND " +
            "(:minPrice IS NULL OR price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR price <= :maxPrice) " +
            "GROUP BY model) " +
            "ORDER BY b.id DESC",
            countQuery = "SELECT COUNT(DISTINCT b.model) FROM bike_model b WHERE " +
                    "(:keyword IS NULL OR :keyword = '' OR b.brand LIKE CONCAT('%', :keyword, '%') OR b.brand_cn LIKE CONCAT('%', :keyword, '%') OR b.model LIKE CONCAT('%', :keyword, '%')) AND " +
                    "(:bikeTypes IS NULL OR :bikeTypes = '' OR b.bike_type IN (:bikeTypesList)) AND " +
                    "(:brands IS NULL OR :brands = '' OR b.brand_cn IN (:brandsList)) AND " +
                    "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
                    "(:maxPrice IS NULL OR b.price <= :maxPrice)",
            nativeQuery = true)
    Page<BikeModel> findDistinctModels(
            @Param("keyword") String keyword,
            @Param("bikeTypes") String bikeTypes,
            @Param("bikeTypesList") List<Integer> bikeTypesList,
            @Param("brands") String brands,
            @Param("brandsList") List<String> brandsList,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable);

    /**
     * 根据ID列表查询车辆（保持原有顺序）
     *
     * @param ids ID列表
     * @return 车辆列表
     */
    List<BikeModel> findByIdIn(List<Long> ids);

    /**
     * 获取所有去重后的车型（每个model只取最小id的记录）
     * 用于推荐算法全量加载，避免内存分组
     *
     * @return 去重后的车型列表
     */
    @Query("SELECT b FROM BikeModel b WHERE b.id IN (SELECT MIN(b2.id) FROM BikeModel b2 GROUP BY b2.model)")
    List<BikeModel> findAllDistinctModels();

    /**
     * 在数据库层批量计算所有车型的几何匹配评分和推荐尺码
     * 用一次SQL查询替代Java侧的遍历计算，消除JVM内存开销。
     * LEFT JOIN确保没有stand_over_height的车型也能返回（默认50分）。
     * GROUP_CONCAT + SUBSTRING_INDEX 高效获取最佳评分对应的尺码。
     *
     * @param inseam 用户跨高
     * @return [modelName(String), bestScore(Integer), recommendedSize(String)]
     */
    @Query(value = "SELECT " +
            "bm.model, " +
            "COALESCE(MAX(CASE " +
            "    WHEN :inseam - s.stand_over_height < 0 THEN 0 " +
            "    WHEN :inseam - s.stand_over_height < 10 THEN 30 " +
            "    WHEN :inseam - s.stand_over_height < 20 THEN 60 " +
            "    WHEN :inseam - s.stand_over_height < 40 THEN 85 " +
            "    WHEN :inseam - s.stand_over_height < 60 THEN 100 " +
            "    ELSE 90 END), 50), " +
            "COALESCE(SUBSTRING_INDEX(GROUP_CONCAT(s.size ORDER BY " +
            "    CASE WHEN :inseam - s.stand_over_height < 0 THEN 0 " +
            "         WHEN :inseam - s.stand_over_height < 10 THEN 30 " +
            "         WHEN :inseam - s.stand_over_height < 20 THEN 60 " +
            "         WHEN :inseam - s.stand_over_height < 40 THEN 85 " +
            "         WHEN :inseam - s.stand_over_height < 60 THEN 100 " +
            "         ELSE 90 END DESC, s.id ASC " +
            "SEPARATOR ','), ',', 1), '') " +
            "FROM bike_model bm " +
            "LEFT JOIN bike_model s ON s.model = bm.model AND s.stand_over_height IS NOT NULL " +
            "WHERE bm.id IN (SELECT MIN(id) FROM bike_model GROUP BY model) " +
            "GROUP BY bm.model",
            nativeQuery = true)
    List<Object[]> computeGeometryScores(@Param("inseam") int inseam);

    /**
     * 批量查询同型号所有尺码（按model名称分组）
     * 用于推荐算法预加载几何数据
     *
     * @param modelNames 型号名称列表
     * @return 所有匹配的车型记录
     */
    @Query("SELECT b FROM BikeModel b WHERE b.model IN :modelNames ORDER BY b.model, b.size")
    List<BikeModel> findByModelIn(@Param("modelNames") List<String> modelNames);
}
