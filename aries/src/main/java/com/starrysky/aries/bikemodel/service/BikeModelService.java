package com.starrysky.aries.bikemodel.service;

import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.bikemodel.repository.BikeModelStatsRepository;
import com.starrysky.aries.bikemodel.schema.BikeModelDTO;
import com.starrysky.aries.bikemodel.schema.BikeModelSearchVO;
import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import com.starrysky.aries.bikemodel.schema.CategoryVO;
import com.starrysky.aries.bikemodel.schema.ShowroomResponseDTO;
import com.starrysky.aries.entity.BikeModel;
import com.starrysky.aries.entity.BikeModelStats;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBikeFavorite;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.userbike.repository.UserBikeFavoriteRepository;
import com.starrysky.aries.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 单车模型服务
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BikeModelService {

    private final BikeModelRepository bikeModelRepository;
    private final BikeModelStatsRepository bikeModelStatsRepository;
    @Qualifier("userBikeUserBikeFavoriteRepository")
    private final UserBikeFavoriteRepository userBikeFavoriteRepository;
    private final UserRepository userRepository;

    /**
     * 模糊搜索品牌和型号名
     *
     * @param keyword 搜索关键字
     * @return 搜索结果
     */
    public List<BikeModelSearchVO> searchModels(String keyword) {
        List<Object[]> results = bikeModelRepository.findDistinctBrandAndModelByKeyword(keyword);
        return results.stream()
                .map(row -> new BikeModelSearchVO((String) row[0], (String) row[1], (String) row[2]))
                .collect(Collectors.toList());
    }

    /**
     * 获取单车模型树
     *
     * @param brand 品牌
     * @param model 型号
     * @return 单车模型树
     */
    public List<BikeModelVO> getModelTree(String brand, String model) {
        List<BikeModel> models = bikeModelRepository.findByBrandAndModel(brand, model);
        return models.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 保存单车模型
     *
     * @param dto 模型数据
     * @return 模型ID
     */
    @Transactional
    public Long saveModel(BikeModelDTO dto) {
        BikeModel bikeModel;
        if (dto.getId() != null) {
            bikeModel = bikeModelRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("BikeModel not found"));
        } else {
            bikeModel = new BikeModel();
        }
        BeanUtils.copyProperties(dto, bikeModel);
        // 特殊字段映射 (VO/DTO 字段名不一致的情况)
        if (dto.getStack() != null) bikeModel.setStack(dto.getStack());
        if (dto.getReach() != null) bikeModel.setReach(dto.getReach());
        if (dto.getStandOverHeight() != null) bikeModel.setStandOverHeight(dto.getStandOverHeight());

        return bikeModelRepository.save(bikeModel).getId();
    }

    /**
     * 删除单车模型
     *
     * @param id 模型ID
     */
    @Transactional
    public void deleteModel(Long id) {
        bikeModelRepository.deleteById(id);
    }

    /**
     * 获取单车展厅列表（分页）
     *
     * @param keyword 搜索关键字
     * @param bikeTypes 单车类型筛选（逗号分隔）
     * @param brands 品牌筛选（逗号分隔）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortBy 排序方式
     * @param pageable 分页参数
     * @return 展厅单车分页数据
     */
    public ShowroomResponseDTO getShowroomBikes(String keyword, String bikeTypes, String brands,
                                                Integer minPrice, Integer maxPrice, String sortBy, Pageable pageable) {
        // 转换 bikeTypes 为整数列表
        List<Integer> bikeTypeList = new ArrayList<>();
        if (StringUtils.hasText(bikeTypes)) {
            bikeTypeList = Arrays.stream(bikeTypes.split(","))
                    .map(String::trim)
                    .filter(s -> StringUtils.hasText(s) && !"all".equals(s))
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
        }

        // 转换 brands 为字符串列表
        List<String> brandList = new ArrayList<>();
        if (StringUtils.hasText(brands)) {
            brandList = Arrays.stream(brands.split(","))
                    .map(String::trim)
                    .filter(s -> StringUtils.hasText(s) && !"all".equals(s))
                    .collect(Collectors.toList());
        }

        // 使用数据库层面去重查询，每个model只返回一条记录
        Page<BikeModel> page = bikeModelRepository.findDistinctModels(keyword, bikeTypes, bikeTypeList, brands, brandList, minPrice, maxPrice, pageable);

        List<BikeModelVO> bikeVOs = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return ShowroomResponseDTO.builder()
                .bikes(bikeVOs)
                .brands(getAllBrands())
                .categories(getAllCategories())
                .total(page.getTotalElements())
                .build();
    }

    /**
     * 获取所有品牌列表
     *
     * @return 品牌列表
     */
    public List<String> getAllBrands() {
        return bikeModelRepository.findDistinctBrands();
    }

    /**
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    public List<CategoryVO> getAllCategories() {
        // 硬编码分类数据，与前端期望的结构一致
        return Arrays.asList(
                CategoryVO.builder().id("1").name("公路车").icon("fas fa-route").build(),
                CategoryVO.builder().id("2").name("山地车").icon("fas fa-tree").build(),
                CategoryVO.builder().id("3").name("Gravel").icon("fas fa-mountain").build(),
                CategoryVO.builder().id("4").name("城市车").icon("fas fa-city").build()
        );
    }

    /**
     * 获取单车详情
     *
     * @param id 单车ID
     * @return 单车详情
     */
    /**
     * 获取单车详情（同时增加浏览量）
     *
     * @param id 单车ID
     * @return 单车详情
     */
    @Transactional
    public BikeModelVO getBikeDetail(Long id) {
        BikeModel bikeModel = bikeModelRepository.findById(id)
                .orElseThrow(() -> new BizException("单车不存在"));
        
        // 增加该车型的浏览量
        incrementViewCount(bikeModel);
        
        return convertToVO(bikeModel);
    }
    
    /**
     * 增加车型浏览量
     * 同一车型的所有尺寸共享浏览量统计
     * 策略：使用同一型号中最小id的尺寸作为代表
     *
     * @param bikeModel 单车模型
     */
    private void incrementViewCount(BikeModel bikeModel) {
        try {
            // 找到代表尺寸的id（同一型号中最小id）
            Long representativeId = bikeModelRepository.findRepresentativeByModel(bikeModel.getModel())
                    .map(BikeModel::getId)
                    .orElse(bikeModel.getId()); // 如果找不到，使用当前id
            
            // 先尝试增加浏览量（如果记录已存在）
            int updated = bikeModelStatsRepository.incrementViewCount(representativeId);
            
            // 如果没有记录被更新，说明记录不存在，需要创建
            if (updated == 0) {
                BikeModelStats stats = new BikeModelStats();
                stats.setBikeModelId(representativeId);
                stats.setViewCount(1L);
                stats.setFavoriteCount(0L);
                stats.setRecommendWeight(0.0);
                stats.setCreateTime(java.time.LocalDateTime.now());
                bikeModelStatsRepository.save(stats);
            }
        } catch (Exception e) {
            // 浏览量统计失败不影响主流程，仅记录日志
            log.error("增加车型浏览量失败: model={}", bikeModel.getModel(), e);
        }
    }

    /**
     * 根据车型名称获取所有尺寸列表
     *
     * @param modelName 车型名称
     * @param brand 品牌（可选，用于更精确匹配）
     * @return 该车型的所有尺寸列表
     */
    public List<BikeModelVO> getModelSizes(String modelName, String brand) {
        List<BikeModel> models;
        if (StringUtils.hasText(brand)) {
            models = bikeModelRepository.findByModelAndBrandOrderBySize(modelName, brand);
        } else {
            models = bikeModelRepository.findByModelOrderBySize(modelName);
        }
        return models.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 单车模型实体转换为VO
     *
     * @param entity 单车模型实体
     * @return 单车模型VO
     */
    private BikeModelVO convertToVO(BikeModel entity) {
        BikeModelVO vo = new BikeModelVO();
        BeanUtils.copyProperties(entity, vo);
        // 显式赋值 price 字段，确保正确转换
        vo.setPrice(entity.getPrice());
        return vo;
    }

    /**
     * 获取用户收藏的单车ID列表
     *
     * @param userId 用户ID
     * @return 收藏的单车ID列表
     */
    public List<Long> getUserFavoriteIds(Long userId) {
        List<UserBikeFavorite> favorites = userBikeFavoriteRepository.findByUserId(userId, Pageable.unpaged()).getContent();
        return favorites.stream()
                .map(favorite -> favorite.getBikeModel().getId())
                .collect(Collectors.toList());
    }

    /**
     * 获取用户收藏的车辆详情列表
     *
     * @param ids 车辆ID列表（逗号分隔）
     * @return 收藏的车辆详情列表
     */
    public ShowroomResponseDTO getFavoriteBikeDetails(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<BikeModel> bikes = bikeModelRepository.findAllById(idList);
        List<BikeModelVO> voList = bikes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        ShowroomResponseDTO response = ShowroomResponseDTO.builder()
                .bikes(voList)
                .total((long) voList.size())
                .build();
        return response;
    }

    /**
     * 切换单车收藏状态
     *
     * @param userId 用户ID
     * @param bikeId 单车ID
     * @return 是否已收藏
     */
    @Transactional
    public Boolean toggleFavorite(Long userId, Long bikeId) {
        var existing = userBikeFavoriteRepository.findByUserIdAndBikeModelId(userId, bikeId);
        
        if (existing.isPresent()) {
            // 已收藏，取消收藏
            userBikeFavoriteRepository.delete(existing.get());
            
            // 更新收藏统计
            BikeModel bikeModel = bikeModelRepository.findById(bikeId).orElse(null);
            if (bikeModel != null) {
                Long representativeId = bikeModelRepository.findRepresentativeByModel(bikeModel.getModel())
                        .map(BikeModel::getId)
                        .orElse(bikeId);
                bikeModelStatsRepository.decrementFavoriteCount(representativeId);
            }
            
            log.info("用户 {} 取消收藏单车 {}", userId, bikeId);
            return false;
        } else {
            // 未收藏，添加收藏
            BikeModel bikeModel = bikeModelRepository.findById(bikeId)
                    .orElseThrow(() -> new BizException("单车不存在"));
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BizException("用户不存在"));
            
            UserBikeFavorite favorite = new UserBikeFavorite();
            favorite.setUser(user);
            favorite.setBikeModel(bikeModel);
            userBikeFavoriteRepository.save(favorite);
            
            // 更新收藏统计
            Long representativeId = bikeModelRepository.findRepresentativeByModel(bikeModel.getModel())
                    .map(BikeModel::getId)
                    .orElse(bikeId);
            
            int updated = bikeModelStatsRepository.incrementFavoriteCount(representativeId);
            if (updated == 0) {
                BikeModelStats stats = new BikeModelStats();
                stats.setBikeModelId(representativeId);
                stats.setViewCount(0L);
                stats.setFavoriteCount(1L);
                stats.setRecommendWeight(0.0);
                stats.setCreateTime(java.time.LocalDateTime.now());
                bikeModelStatsRepository.save(stats);
            }
            
            log.info("用户 {} 收藏单车 {}", userId, bikeId);
            return true;
        }
    }
}
