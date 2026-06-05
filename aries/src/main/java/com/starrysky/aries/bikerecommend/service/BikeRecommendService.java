package com.starrysky.aries.bikerecommend.service;

import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendCacheDTO;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendRequest;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendVO;
import com.starrysky.aries.bikerecommend.schema.BikeStandOverCacheItem;
import com.starrysky.aries.entity.BikeModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单车智能推荐服务
 * 基于多维度评分算法为用户推荐最适合的单车
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BikeRecommendService {

    private final BikeModelRepository bikeModelRepository;
    private final BikeRecommendCacheService cacheService;

    /**
     * 场景-车型匹配评分矩阵
     * 行: 场景ID(1-5), 列: 车型ID(1-5)
     * 场景: 1-日常通勤, 2-运动健身, 3-山地越野, 4-长途旅行, 5-速度竞技
     * 车型: 1-公路, 2-山地, 3-Gravel, 4-城市, 5-TT
     */
    private static final int[][] SCENE_BIKE_MATRIX = {
            {0, 70, 40, 75, 100, 0},   // 场景1: 日常通勤
            {0, 95, 70, 90, 75, 40},   // 场景2: 运动健身
            {0, 10, 100, 45, 0, 0},    // 场景3: 山地越野
            {0, 85, 50, 100, 70, 0},   // 场景4: 长途旅行
            {0, 100, 0, 40, 0, 95}     // 场景5: 速度竞技
    };

    /**
     * 价格预过滤浮动范围（元）
     * 用户指定价格区间时，保留区间外±500元以内的车型参与后续评分
     */
    private static final int PRICE_FLOAT_MARGIN = 500;

    /**
     * 执行智能推荐算法
     *
     * @param request 推荐请求参数
     * @return 按综合评分降序排列的推荐单车列表(最多15辆)
     */
    public List<BikeRecommendVO> recommend(BikeRecommendRequest request) {
        // 1. 计算跨高(如未填写则使用身高*0.45)
        int inseam = request.getInseam() != null ? request.getInseam() : (int) (request.getHeight() * 0.45);
        log.info("推荐参数: height={}, inseam={}, scenes={}, priceRange=[{}, {}]",
                request.getHeight(), inseam, request.getScenes(), request.getMinPrice(), request.getMaxPrice());

        // 2. 加载缓存数据（热度分 + 跨高几何数据，均为 Redis Hash/String）
        Map<String, BikeRecommendCacheDTO> cacheMap = cacheService.loadRecommendCache();
        Map<String, List<BikeStandOverCacheItem>> standOverCache = cacheService.loadStandOverCache();
        log.info("缓存加载完成: 热度分 {} 个型号, 跨高数据 {} 个车型",
                cacheMap.size(), standOverCache.size());

        // 3. 查询所有单车模型（仍需要完整数据用于 VO 输出和其他评分维度）
        List<BikeModel> allModels = getAllDistinctModels();
        log.info("共查询到 {} 个不同型号的单车", allModels.size());

        // 3.5 价格区间预过滤
        allModels = filterByPriceRange(allModels, request.getMinPrice(), request.getMaxPrice());

        // 4. 在内存中计算几何评分（基于缓存的 stand_over_height，无需 SQL）
        List<String> modelNames = allModels.stream().map(BikeModel::getModel).collect(Collectors.toList());
        Map<String, GeometryResult> geometryScoreMap = computeGeometryScoresInMemory(inseam, modelNames, standOverCache);
        log.info("预计算了 {} 个型号的几何评分", geometryScoreMap.size());

        // 5. 对每款车计算评分（热度分从缓存读取，无需查询统计表）
        List<BikeRecommendVO> results = new ArrayList<>();
        for (BikeModel model : allModels) {
            try {
                BikeRecommendCacheDTO cacheItem = cacheMap.get(model.getModel());
                BikeRecommendVO vo = calculateScores(model, inseam, request, cacheItem, geometryScoreMap);
                results.add(vo);
            } catch (Exception e) {
                log.error("计算单车评分失败: model={}, error={}", model.getModel(), e.getMessage(), e);
            }
        }

        // 6. 按totalScore降序排序
        results.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));

        // 7. 限制返回数量为前15辆
        int maxResults = 15;
        if (results.size() > maxResults) {
            results = results.subList(0, maxResults);
        }

        log.info("推荐计算完成,返回 {} 个结果", results.size());
        return results;
    }

    /**
     * 获取所有去重后的单车模型（DB层去重，避免内存分组和全表扫描）
     */
    private List<BikeModel> getAllDistinctModels() {
        return bikeModelRepository.findAllDistinctModels();
    }

    /**
     * 按价格区间预过滤车型列表
     */
    private List<BikeModel> filterByPriceRange(List<BikeModel> models, Integer minPrice, Integer maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return models;
        }

        int beforeCount = models.size();
        List<BikeModel> filtered = models.stream()
                .filter(m -> {
                    Integer price = m.getPrice();
                    if (price == null) {
                        return false;
                    }
                    boolean aboveMin = minPrice == null || price >= (minPrice - PRICE_FLOAT_MARGIN);
                    boolean belowMax = maxPrice == null || price <= (maxPrice + PRICE_FLOAT_MARGIN);
                    return aboveMin && belowMax;
                })
                .collect(Collectors.toList());

        int removed = beforeCount - filtered.size();
        if (removed > 0) {
            log.info("价格预过滤: 砍除 {} 个车型(价格区间[{},{}]±{}元), 剩余 {} 个参与评分",
                    removed, minPrice, maxPrice, PRICE_FLOAT_MARGIN, filtered.size());
        } else {
            log.info("价格预过滤: 全部 {} 个车型均在价格区间内, 无需砍除", filtered.size());
        }
        return filtered;
    }

    /**
     * 在内存中计算指定车型的几何评分（基于缓存的 stand_over_height 数据）
     *
     * 替代原有的 SQL 自 JOIN 查询。评分逻辑与原 SQL 完全一致：
     * clearance = inseam - stand_over_height
     * <0→0分, <10→30分, <20→60分, <40→85分, <60→100分, ≥60→90分
     *
     * @param inseam        用户跨高
     * @param modelNames    需要计算的车型名称
     * @param standOverCache 缓存的跨高数据
     */
    private Map<String, GeometryResult> computeGeometryScoresInMemory(int inseam,
                                                                       List<String> modelNames,
                                                                       Map<String, List<BikeStandOverCacheItem>> standOverCache) {
        Map<String, GeometryResult> map = new HashMap<>();
        for (String modelName : modelNames) {
            List<BikeStandOverCacheItem> variants = standOverCache.get(modelName);
            if (variants == null || variants.isEmpty()) {
                map.put(modelName, new GeometryResult(50, 50, null));
                continue;
            }

            int bestScore = 0;
            String bestSize = null;

            for (BikeStandOverCacheItem variant : variants) {
                int clearance = inseam - variant.getStandOverHeight();
                int score;
                if (clearance < 0) {
                    score = 0;
                } else if (clearance < 10) {
                    score = 30;
                } else if (clearance < 20) {
                    score = 60;
                } else if (clearance < 40) {
                    score = 85;
                } else if (clearance < 60) {
                    score = 100;
                } else {
                    score = 90;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestSize = variant.getSize();
                }
            }

            map.put(modelName, new GeometryResult(bestScore, bestScore, bestSize));
        }
        return map;
    }

    /**
     * 计算单车综合评分
     *
     * @param cacheItem 缓存数据（含预计算热度分），为null时热度分默认0
     */
    private BikeRecommendVO calculateScores(BikeModel model, int inseam,
                                            BikeRecommendRequest request,
                                            BikeRecommendCacheDTO cacheItem,
                                            Map<String, GeometryResult> geometryScoreMap) {
        BikeRecommendVO vo = convertToRecommendVO(model);

        // 计算各维度评分
        double sceneScore = calculateSceneScore(model.getBikeType(), request.getScenes());
        GeometryResult geometryResult = geometryScoreMap.get(model.getModel());
        if (geometryResult == null) {
            geometryResult = new GeometryResult(50, 50, null);
        }
        double hotScore = cacheItem != null ? cacheItem.getHotScore() : 0.0;
        double priceScore = calculatePriceScore(model.getPrice(), request.getMinPrice(), request.getMaxPrice());

        // 计算综合评分
        boolean hasPriceFilter = request.getMinPrice() != null || request.getMaxPrice() != null;
        double totalScore;

        if (hasPriceFilter) {
            totalScore = sceneScore * 0.30 + geometryResult.score * 0.40 + hotScore * 0.15 + priceScore * 0.15;
        } else {
            totalScore = (sceneScore * 0.30 + geometryResult.score * 0.40 + hotScore * 0.15) / 0.85;
        }

        vo.setTotalScore(round(totalScore, 2));
        vo.setSceneScore(round(sceneScore, 2));
        vo.setGeometryScore(round(geometryResult.score, 2));
        vo.setHotScore(round(hotScore, 2));
        vo.setPriceScore(hasPriceFilter ? round(priceScore, 2) : null);
        vo.setBestSizeScore(geometryResult.bestSizeScore);
        vo.setRecommendedSize(geometryResult.recommendedSize);

        return vo;
    }

    /**
     * 计算场景匹配评分(多场景取平均)
     */
    private double calculateSceneScore(Integer bikeType, List<Integer> scenes) {
        if (bikeType == null || scenes == null || scenes.isEmpty()) {
            return 0;
        }

        double totalScore = 0;
        int validSceneCount = 0;

        for (Integer sceneId : scenes) {
            if (sceneId >= 1 && sceneId <= 5 && bikeType >= 1 && bikeType <= 5) {
                totalScore += SCENE_BIKE_MATRIX[sceneId - 1][bikeType];
                validSceneCount++;
            }
        }

        return validSceneCount > 0 ? totalScore / validSceneCount : 0;
    }

    /**
     * 几何匹配评分结果
     */
    private static class GeometryResult {
        double score;
        int bestSizeScore;
        String recommendedSize;

        GeometryResult(double score, int bestSizeScore, String recommendedSize) {
            this.score = score;
            this.bestSizeScore = bestSizeScore;
            this.recommendedSize = recommendedSize;
        }
    }

    /**
     * 计算价格匹配评分
     */
    private double calculatePriceScore(Integer bikePrice, Integer minPrice, Integer maxPrice) {
        if (bikePrice == null || (minPrice == null && maxPrice == null)) {
            return 100;
        }

        boolean aboveMin = (minPrice == null || bikePrice >= minPrice);
        boolean belowMax = (maxPrice == null || bikePrice <= maxPrice);
        if (aboveMin && belowMax) {
            return 100;
        }

        double diffRate;
        if (minPrice != null && bikePrice < minPrice) {
            diffRate = (double) (minPrice - bikePrice) / minPrice;
        } else if (maxPrice != null && bikePrice > maxPrice) {
            diffRate = (double) (bikePrice - maxPrice) / maxPrice;
        } else {
            return 100;
        }

        return Math.max(100 - diffRate * 100, 40);
    }

    /**
     * 转换为推荐VO
     */
    private BikeRecommendVO convertToRecommendVO(BikeModel model) {
        BikeRecommendVO vo = new BikeRecommendVO();
        BeanUtils.copyProperties(model, vo);
        vo.setPrice(model.getPrice());
        return vo;
    }

    /**
     * 四舍五入到指定小数位
     */
    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
