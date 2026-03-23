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

            int score = BiometricsCalculator.calculateFlexibilityScore(
                    latestBody.getThoracicRotationAngle(),
                    latestBody.getArmRaiseAngle(),
                    latestBody.getThoracicPainLevel()
            );
            vo.setFlexibilityScore(score);
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
                bikeSummaryVO.setBrand(bike.getBikeModel().getBrand());
                bikeSummaryVO.setBrandCn(bike.getBikeModel().getBrandCn());
                bikeSummaryVO.setModel(bike.getBikeModel().getModel());
                bikeSummaryVO.setModelYear(bike.getBikeModel().getModelYear());
                bikeSummaryVO.setBikeType(bike.getBikeModel().getBikeType());
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
