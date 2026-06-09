package com.starrysky.aries.bikerecommend.service;

import cn.hutool.json.JSONUtil;
import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.bikemodel.repository.BikeModelStatsRepository;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendCacheDTO;
import com.starrysky.aries.bikerecommend.schema.BikeStandOverCacheItem;
import com.starrysky.aries.entity.BikeModel;
import com.starrysky.aries.entity.BikeModelStats;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推荐缓存服务
 * 管理推荐热度分和跨高几何数据的 Redis 缓存，减少 DB 查询
 *
 * @author adi
 * @since 2026/06/01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BikeRecommendCacheService {

    private final RedisUtil redisUtil;
    private final BikeModelRepository bikeModelRepository;
    private final BikeModelStatsRepository statsRepository;

    // ==================== 热度分缓存 ====================

    /**
     * 获取缓存的推荐数据（model -> BikeRecommendCacheDTO）
     * 优先从 Redis Hash 读取，缓存未命中时从 DB 加载并回写缓存
     *
     * @return model名称 -> 缓存数据的映射
     */
    @SuppressWarnings("unchecked")
    public Map<String, BikeRecommendCacheDTO> loadRecommendCache() {
        String cacheKey = RedisConstant.RECOMMEND_CACHE;

        // 1. 尝试从 Redis Hash 加载
        if (redisUtil.hasKey(cacheKey)) {
            Map<Object, Object> cachedMap = redisUtil.hmget(cacheKey);
            if (cachedMap != null && !cachedMap.isEmpty()) {
                Map<String, BikeRecommendCacheDTO> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : cachedMap.entrySet()) {
                    try {
                        BikeRecommendCacheDTO dto = JSONUtil.toBean(
                                entry.getValue().toString(), BikeRecommendCacheDTO.class);
                        result.put(entry.getKey().toString(), dto);
                    } catch (Exception e) {
                        log.warn("反序列化推荐缓存项失败: field={}", entry.getKey(), e);
                    }
                }
                if (!result.isEmpty()) {
                    log.info("从 Redis 加载推荐缓存, 共 {} 条", result.size());
                    return result;
                }
            }
        }

        // 2. 缓存未命中，从 DB 加载并计算热度分
        log.info("推荐缓存未命中，从 DB 加载并计算热度分");
        return computeAndCache();
    }

    /**
     * 从 DB 加载去重车型数据，计算热度分，写入 Redis
     */
    private Map<String, BikeRecommendCacheDTO> computeAndCache() {
        List<BikeModel> allModels = bikeModelRepository.findAllDistinctModels();
        if (allModels.isEmpty()) {
            log.warn("未查询到任何单车模型，跳过缓存构建");
            return new HashMap<>();
        }

        // 批量加载统计数据
        List<Long> modelIds = allModels.stream().map(BikeModel::getId).toList();
        Map<Long, BikeModelStats> statsMap = statsRepository.findByBikeModelIdIn(modelIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        BikeModelStats::getBikeModelId, s -> s, (a, b) -> a));

        Map<String, BikeRecommendCacheDTO> result = new HashMap<>();
        Map<String, Object> redisHashData = new HashMap<>();

        for (BikeModel model : allModels) {
            BikeModelStats stats = statsMap.get(model.getId());
            double hotScore = calculateHotScore(model, stats);

            BikeRecommendCacheDTO dto = new BikeRecommendCacheDTO(
                    model.getId(), model.getModel(), model.getPrice(), hotScore);
            result.put(model.getModel(), dto);

            // 存入 Redis Hash 的序列化数据
            redisHashData.put(model.getModel(), JSONUtil.toJsonStr(dto));
        }

        // 批量写入 Redis Hash，设置 TTL
        String cacheKey = RedisConstant.RECOMMEND_CACHE;
        if (!redisHashData.isEmpty()) {
            redisUtil.hmset(cacheKey, redisHashData, RedisConstant.RECOMMEND_CACHE_TIMEOUT);
            log.info("推荐缓存已写入 Redis, 共 {} 条, TTL={}s", redisHashData.size(),
                    RedisConstant.RECOMMEND_CACHE_TIMEOUT);
        }

        return result;
    }

    // ==================== 跨高数据缓存 ====================

    /**
     * 获取缓存的跨高数据（model -> List&lt;BikeStandOverCacheItem&gt;）
     * 纯静态几何数据，不限 TTL，仅在车型数据更新后清除重建。
     * 优先从 Redis 读取，缓存未命中时从 DB 加载全量车型的尺码×跨高数据。
     *
     * @return model名称 -> 该车型所有尺码的跨高数据列表
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<BikeStandOverCacheItem>> loadStandOverCache() {
        String cacheKey = RedisConstant.RECOMMEND_STAND_OVER_CACHE;

        // 1. 尝试从 Redis 加载
        if (redisUtil.hasKey(cacheKey)) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                try {
                    String json = cached.toString();
                    Map<String, List<BikeStandOverCacheItem>> result = JSONUtil.toBean(
                            json,
                            new cn.hutool.core.lang.TypeReference<Map<String, List<BikeStandOverCacheItem>>>() {},
                            false);
                    if (result != null && !result.isEmpty()) {
                        log.info("从 Redis 加载跨高缓存, 共 {} 个车型", result.size());
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("反序列化跨高缓存失败", e);
                }
            }
        }

        // 2. 缓存未命中，从 DB 加载
        log.info("跨高缓存未命中，从 DB 加载");
        return computeStandOverCache();
    }

    /**
     * 从 DB 加载所有车型的尺码×跨高数据，写入 Redis（无 TTL，永久有效）
     */
    private Map<String, List<BikeStandOverCacheItem>> computeStandOverCache() {
        // 查询所有有跨高数据的车型记录
        List<BikeModel> allModels = bikeModelRepository.findAllDistinctModels();
        // 根据去重后的 model 名，批量查所有尺寸变体
        List<String> modelNames = allModels.stream().map(BikeModel::getModel).distinct().collect(Collectors.toList());
        List<BikeModel> allVariants = bikeModelRepository.findByModelIn(modelNames);

        // 按 model 分组，提取 size + stand_over_height
        Map<String, List<BikeStandOverCacheItem>> result = new HashMap<>();
        for (BikeModel variant : allVariants) {
            if (variant.getStandOverHeight() == null) {
                continue;
            }
            result.computeIfAbsent(variant.getModel(), k -> new ArrayList<>())
                    .add(new BikeStandOverCacheItem(variant.getSize(), variant.getStandOverHeight()));
        }

        // 写入 Redis（无 TTL，纯静态数据）
        if (!result.isEmpty()) {
            String json = JSONUtil.toJsonStr(result);
            redisUtil.set(RedisConstant.RECOMMEND_STAND_OVER_CACHE, json);
            log.info("跨高缓存已写入 Redis, 共 {} 个车型", result.size());
        }

        return result;
    }

    // ==================== 缓存管理 ====================

    /**
     * 清除所有推荐缓存（热度分 + 跨高数据，统计数据或车型数据更新后调用）
     */
    public void clearCache() {
        redisUtil.del(RedisConstant.RECOMMEND_CACHE, RedisConstant.RECOMMEND_STAND_OVER_CACHE);
        log.info("推荐缓存已全部清除");
    }

    /**
     * 仅清除热度分缓存（统计数据更新后调用）
     */
    public void clearHotScoreCache() {
        redisUtil.del(RedisConstant.RECOMMEND_CACHE);
        log.info("热度分缓存已清除");
    }

    /**
     * 仅清除跨高数据缓存（车型几何数据更新后调用）
     */
    public void clearStandOverCache() {
        redisUtil.del(RedisConstant.RECOMMEND_STAND_OVER_CACHE);
        log.info("跨高数据缓存已清除");
    }

    // ==================== 私有工具方法 ====================

    /**
     * 计算热度评分
     */
    private double calculateHotScore(BikeModel model, BikeModelStats stats) {
        long viewCount = stats != null && stats.getViewCount() != null ? stats.getViewCount() : 0;
        long favoriteCount = stats != null && stats.getFavoriteCount() != null ? stats.getFavoriteCount() : 0;
        double recommendWeight = stats != null && stats.getRecommendWeight() != null ? stats.getRecommendWeight() : 0.0;

        double viewScore = Math.min(viewCount / 20.0, 100);
        double favoriteScore = Math.min(favoriteCount * 5, 100);

        int currentYear = LocalDate.now().getYear();
        int bikeYear = parseModelYear(model.getModelYear());
        int yearDiff = currentYear - bikeYear;
        double newScore = Math.max(100 - yearDiff * 15, 20);

        return viewScore * 0.25 + favoriteScore * 0.35 + newScore * 0.35 + Math.min(recommendWeight * 0.05, 5);
    }

    /**
     * 解析车型年份
     */
    private int parseModelYear(String modelYear) {
        if (modelYear == null || modelYear.isEmpty()) {
            return 2024;
        }
        try {
            return Integer.parseInt(modelYear);
        } catch (NumberFormatException e) {
            log.warn("车型年份解析失败: {}, 使用默认值2024", modelYear);
            return 2024;
        }
    }
}
