package com.starrysky.aries.lab.service;

import com.starrysky.aries.dynamicfitting.repository.DynamicFittingResultRepository;
import com.starrysky.aries.entity.UserBike;
import com.starrysky.aries.entity.UserBody;
import com.starrysky.aries.lab.schema.LabDashboardVO;
import com.starrysky.aries.staticfitting.repository.StaticFittingResultRepository;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import com.starrysky.aries.userbody.repository.UserBodyRepository;
import com.starrysky.aries.userbody.service.BiometricsCalculator;
import com.starrysky.aries.util.BikeStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实验室服务
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/18 20:05
 */
@Service
public class LabService {
    @Resource
    private UserBodyRepository userBodyRepository;
    @Resource
    private UserBikeRepository userBikeInfoRepository;
    @Resource
    private StaticFittingResultRepository staticFittingResultRepository;
    @Resource
    private DynamicFittingResultRepository dynamicFittingResultRepository;

    public LabDashboardVO getDashboard(Long userId) {
        LabDashboardVO vo = new LabDashboardVO();

        // 1. 获取身体概要 (获取最新的一条身体数据)
        UserBody latestBody = userBodyRepository.findTopByUserIdOrderByIdDesc(userId)
                .orElse(null);

        Long latestBodyId = null;
        if (latestBody != null) {
            vo.setHasBodyProfile(true);
            latestBodyId = latestBody.getId();

            // 柔韧性评分：需要三个字段都存在才计算，避免 NPE
            Double thoracicRotationAngle = latestBody.getThoracicRotationAngle();
            Double armRaiseAngle = latestBody.getArmRaiseAngle();
            Integer thoracicPainLevel = latestBody.getThoracicPainLevel();
            if (thoracicRotationAngle != null && armRaiseAngle != null && thoracicPainLevel != null) {
                int score = BiometricsCalculator.calculateFlexibilityScore(
                        thoracicRotationAngle,
                        armRaiseAngle,
                        thoracicPainLevel
                );
                vo.setFlexibilityScore(score);
            }

            // 计算稳定性评分 (深蹲60% + 平板支撑40%，转换为0-100分)
            Integer squatScore = latestBody.getSquatStabilityScore();
            Integer plankScore = latestBody.getPlankScore();
            if (squatScore != null && squatScore > 0) {
                double coreScore = squatScore * 0.6;
                if (plankScore != null && plankScore > 0) {
                    coreScore += plankScore * 0.4;
                }
                vo.setStabilityScore((int) Math.round(coreScore * 20));
            }
        }

        // 2. 获取车辆列表及其(静态/动态)报告状态
        List<UserBike> bikes = userBikeInfoRepository.findByUserIdAndStatusOrderByCreateTimeDesc(userId, BikeStatus.ACTIVE.code);

        if (bikes != null && !bikes.isEmpty()) {
            vo.setBikeCount(bikes.size());

            // 批量获取当前身体档案下，已完成【静态】Fitting 的车辆 IDs
            Set<Long> staticFittedBikeIds = new HashSet<>();
            // 批量获取当前身体档案下，已完成【动态】Fitting 的车辆 IDs
            Set<Long> dynamicFittedBikeIds = new HashSet<>();

            if (latestBodyId != null) {
                staticFittedBikeIds = staticFittingResultRepository.findBikeIdsByUserId(userId);
                dynamicFittedBikeIds = dynamicFittingResultRepository.findBikeIdsByUserId(userId);
            }

            Set<Long> finalStaticIds = staticFittedBikeIds;
            Set<Long> finalDynamicIds = dynamicFittedBikeIds;

            List<LabDashboardVO.BikeSummaryVO> bikesvo = bikes.stream().map(bike -> {
                LabDashboardVO.BikeSummaryVO bikeSummaryVO = new LabDashboardVO.BikeSummaryVO();
                bikeSummaryVO.setId(bike.getId());
                
                // 容错处理：捕获 BikeModel 被删除的异常
                try {
                    if (bike.getBikeModel() != null) {
                        bikeSummaryVO.setBrand(bike.getBikeModel().getBrand());
                        bikeSummaryVO.setBrandCn(bike.getBikeModel().getBrandCn());
                        bikeSummaryVO.setModel(bike.getBikeModel().getModel());
                        bikeSummaryVO.setModelYear(bike.getBikeModel().getModelYear());
                        bikeSummaryVO.setBikeType(bike.getBikeModel().getBikeType());
                    }
                } catch (Exception e) {
                    // 如果 BikeModel 已被删除，设置默认值
                    bikeSummaryVO.setBrand("Unknown");
                    bikeSummaryVO.setBrandCn("未知品牌");
                    bikeSummaryVO.setModel("未知型号");
                }
                
                bikeSummaryVO.setRemark(bike.getRemark());
                // 赋值状态
                bikeSummaryVO.setHasStaticReport(finalStaticIds.contains(bike.getId()));
                bikeSummaryVO.setHasDynamicReport(finalDynamicIds.contains(bike.getId()));

                return bikeSummaryVO;
            }).collect(Collectors.toList());

            vo.setBikes(bikesvo);

        }
        return vo;
    }
}
