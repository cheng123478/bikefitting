package com.starrysky.aries.bikerecommend.service;

import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.bikemodel.repository.BikeModelStatsRepository;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendRequest;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendVO;
import com.starrysky.aries.entity.BikeModel;
import com.starrysky.aries.entity.BikeModelStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final BikeModelStatsRepository statsRepository;

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
     * 执行智能推荐算法
     *
     * @param request 推荐请求参数
     * @return 按综合评分降序排列的推荐单车列表
     */
    public List<BikeRecommendVO> recommend(BikeRecommendRequest request) {
        // 1. 计算跨高(如未填写则使用身高*0.45)
        int inseam = request.getInseam() != null ? request.getInseam() : (int) (request.getHeight() * 0.45);
        log.info("推荐参数: height={}, inseam={}, scenes={}, priceRange=[{}, {}]",
                request.getHeight(), inseam, request.getScenes(), request.getMinPrice(), request.getMaxPrice());

        // 2. 查询所有单车模型(按型号去重,每个型号取最小id的记录)
        List<BikeModel> allModels = getAllDistinctModels();
        log.info("共查询到 {} 个不同型号的单车", allModels.size());

        // 3. 批量加载统计数据,避免N+1查询
        Map<Long, BikeModelStats> statsMap = loadStatsBatch(allModels);

        // 4. 对每款车计算评分
        List<BikeRecommendVO> results = new ArrayList<>();
        for (BikeModel model : allModels) {
            try {
                BikeRecommendVO vo = calculateScores(model, inseam, request, statsMap);
                results.add(vo);
            } catch (Exception e) {
                log.error("计算单车评分失败: model={}, error={}", model.getModel(), e.getMessage(), e);
            }
        }

        // 5. 按totalScore降序排序
        results.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));

        log.info("推荐计算完成,返回 {} 个结果", results.size());
        return results;
    }

    /**
     * 获取所有去重后的单车模型(每个型号取最小id)
     */
    private List<BikeModel> getAllDistinctModels() {
        // 查询所有车型
        List<BikeModel> allBikes = bikeModelRepository.findAll();
        
        // 按型号分组,每个型号取最小id的记录
        return allBikes.stream()
                .collect(Collectors.groupingBy(BikeModel::getModel))
                .values()
                .stream()
                .map(list -> list.stream()
                        .min((a, b) -> a.getId().compareTo(b.getId()))
                        .orElse(null))
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    /**
     * 批量加载统计数据
     */
    private Map<Long, BikeModelStats> loadStatsBatch(List<BikeModel> models) {
        Map<Long, BikeModelStats> statsMap = new HashMap<>();
        for (BikeModel model : models) {
            try {
                statsRepository.findByBikeModelId(model.getId())
                        .ifPresent(stats -> statsMap.put(model.getId(), stats));
            } catch (Exception e) {
                log.warn("加载统计数据失败: modelId={}", model.getId(), e);
            }
        }
        return statsMap;
    }

    /**
     * 计算单车综合评分
     */
    private BikeRecommendVO calculateScores(BikeModel model, int inseam,
                                            BikeRecommendRequest request,
                                            Map<Long, BikeModelStats> statsMap) {
        BikeRecommendVO vo = convertToRecommendVO(model);

        // 计算各维度评分
        double sceneScore = calculateSceneScore(model.getBikeType(), request.getScenes());
        GeometryResult geometryResult = calculateGeometryScore(model, inseam);
        double hotScore = calculateHotScore(model, statsMap.get(model.getId()));
        double priceScore = calculatePriceScore(model.getPrice(), request.getMinPrice(), request.getMaxPrice());

        // 计算综合评分
        boolean hasPriceFilter = request.getMinPrice() != null || request.getMaxPrice() != null;
        double totalScore;

        if (hasPriceFilter) {
            // 有价格过滤: 4个维度按权重分配
            totalScore = sceneScore * 0.30 + geometryResult.score * 0.40 + hotScore * 0.15 + priceScore * 0.15;
        } else {
            // 无价格过滤: 重新归一化权重(0.30+0.40+0.15=0.85)
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
            // 验证场景ID和车型ID范围
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
     * 计算几何匹配评分(查询同型号所有尺码,取最大sizeScore)
     */
    private GeometryResult calculateGeometryScore(BikeModel model, int inseam) {
        // 查询同型号所有尺码
        List<BikeModel> sameModelSizes = bikeModelRepository.findByModelOrderBySize(model.getModel());

        int maxScore = 0;
        String bestSize = null;

        for (BikeModel size : sameModelSizes) {
            if (size.getStandOverHeight() != null) {
                int clearance = inseam - size.getStandOverHeight();
                int score = calculateSizeScore(clearance);
                if (score > maxScore) {
                    maxScore = score;
                    bestSize = size.getSize();
                }
            }
        }

        // 如果没有standOverHeight数据,给中间分
        if (bestSize == null) {
            maxScore = 50;
        }

        return new GeometryResult(maxScore, maxScore, bestSize);
    }

    /**
     * 计算sizeScore(基于clearance跨高间隙)
     */
    private int calculateSizeScore(double clearance) {
        if (clearance < 0) return 0;
        if (clearance < 10) return 30;
        if (clearance < 20) return 60;
        if (clearance < 40) return 85;
        if (clearance < 60) return 100;
        return 90; // clearance >= 60
    }

    /**
     * 计算热度评分
     */
    private double calculateHotScore(BikeModel model, BikeModelStats stats) {
        long viewCount = stats != null && stats.getViewCount() != null ? stats.getViewCount() : 0;
        long favoriteCount = stats != null && stats.getFavoriteCount() != null ? stats.getFavoriteCount() : 0;
        double recommendWeight = stats != null && stats.getRecommendWeight() != null ? stats.getRecommendWeight() : 0.0;

        double viewScore = Math.min(viewCount / 20.0, 100);
        double favoriteScore = Math.min(favoriteCount * 5, 100);

        // 计算newScore(基于车龄)
        int currentYear = LocalDate.now().getYear();
        int bikeYear = parseModelYear(model.getModelYear());
        int yearDiff = currentYear - bikeYear;
        double newScore = Math.max(100 - yearDiff * 15, 20);

        return viewScore * 0.2 + favoriteScore * 0.5 + recommendWeight * 1.0 + newScore;
    }

    /**
     * 解析车型年份
     */
    private int parseModelYear(String modelYear) {
        if (modelYear == null || modelYear.isEmpty()) {
            return 2024; // 默认值
        }
        try {
            return Integer.parseInt(modelYear);
        } catch (NumberFormatException e) {
            log.warn("车型年份解析失败: {}, 使用默认值2024", modelYear);
            return 2024;
        }
    }

    /**
     * 计算价格匹配评分
     */
    private double calculatePriceScore(Integer bikePrice, Integer minPrice, Integer maxPrice) {
        // 无价格信息或不填预算时返回100(不参与评分)
        if (bikePrice == null || (minPrice == null && maxPrice == null)) {
            return 100;
        }

        // 单车价格在用户预算区间内
        boolean aboveMin = (minPrice == null || bikePrice >= minPrice);
        boolean belowMax = (maxPrice == null || bikePrice <= maxPrice);
        if (aboveMin && belowMax) {
            return 100;
        }

        // 计算偏差率
        double diffRate;
        if (minPrice != null && bikePrice < minPrice) {
            // 低于最低价
            diffRate = (double) (minPrice - bikePrice) / minPrice;
        } else if (maxPrice != null && bikePrice > maxPrice) {
            // 高于最高价
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
