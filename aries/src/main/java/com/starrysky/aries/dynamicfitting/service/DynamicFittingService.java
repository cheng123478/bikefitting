package com.starrysky.aries.dynamicfitting.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.starrysky.aries.dynamicfitting.algorithm.DynamicFittingAlgorithm;
import com.starrysky.aries.dynamicfitting.algorithm.PedalingSimulationAlgorithm;
import com.starrysky.aries.dynamicfitting.repository.DynamicFittingMotionDataRepository;
import com.starrysky.aries.dynamicfitting.repository.DynamicFittingResultRepository;
import com.starrysky.aries.dynamicfitting.repository.DynamicFittingSessionRepository;
import com.starrysky.aries.dynamicfitting.repository.DynamicFittingSnapshotRepository;
import com.starrysky.aries.dynamicfitting.schema.*;
import com.starrysky.aries.entity.*;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import com.starrysky.aries.userbody.repository.UserBodyRepository;
import com.starrysky.aries.util.FittingStatus;
import com.starrysky.aries.util.RidingPainType;
import com.starrysky.aries.util.upload.CosUtil;
import com.starrysky.aries.util.upload.FileUploadUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态 Fitting 服务
 *
 * @author adice
 * @version 1.0
 * @since 2026-02-08 11:19:57
 */
@Service
@Slf4j
public class DynamicFittingService {

    @Resource
    private DynamicFittingSessionRepository sessionRepository;
    @Resource
    private DynamicFittingMotionDataRepository motionDataRepository;
    @Resource
    private DynamicFittingSnapshotRepository snapshotRepository;
    @Resource
    private DynamicFittingResultRepository resultRepository;
    @Resource
    private UserBikeRepository userBikeInfoRepository;
    @Resource
    private UserBodyRepository userBodyRepository;
    @Resource
    private FileUploadUtil fileUploadUtil;
    @Resource
    private CosUtil cosUtil;
    @Resource
    private DynamicFittingAlgorithm fittingAlgorithm;
    @Resource
    private PedalingSimulationAlgorithm pedalingSimulationAlgorithm;

    /**
     * 保存Fitting会话
     *
     * @param user    用户
     * @param request 请求
     * @return 会话VO对象
     */
    @Transactional(rollbackFor = Exception.class)
    public DynamicFittingSessionVO saveFittingSession(User user, DynamicFittingRequest request) {
        // 1. 创建 Session (同步)
        DynamicFittingSession session = new DynamicFittingSession();
        session.setUser(user);
        session.setUserBike(userBikeInfoRepository.getReferenceById(request.getBikeId()));
        UserBody userBody = userBodyRepository.findTopByUserIdOrderByIdDesc(user.getId()).orElse(null);
        if (userBody == null) {
            throw new BizException("用户身体信息不存在");
        }
        session.setUserBody(userBody);
        session.setStatus(FittingStatus.ANALYSING.getCode()); // 待分析
        session.setSessionTime(request.getSessionTime().toLocalDateTime());
        if (request.getPainLevels() != null && !request.getPainLevels().isEmpty()) {
            session.setPainLevels(JSON.toJSONString(request.getPainLevels()));
        }
        session.setCreateTime(LocalDateTime.now());
        session = sessionRepository.save(session);

        // 2. 异步处理后续逻辑 (轨迹上传、COS上传、算法分析)
        // 注意：由于 @Async 在同一个类中调用不会生效，需要确保通过代理调用。
        // 在 Aries 项目中，如果 DynamicFittingService 是通过 @Resource 注入到 Controller 的，
        // 且 asyncProcessFittingSession 是由 Controller 调用的，则没问题。
        // 把session转换为VO对象
        DynamicFittingSessionVO sessionVO = new DynamicFittingSessionVO();
        BeanUtils.copyProperties(session, sessionVO);
        sessionVO.setBikeId(session.getUserBike().getId());
        sessionVO.setBikeName(
                session.getUserBike().getBikeModel() != null ? session.getUserBike().getBikeModel().getModel()
                        : "未知车型");
        sessionVO.setBodyId(session.getUserBody().getId());
        return sessionVO;
    }

    /**
     * 异步处理Fitting会话
     *
     * @param sessionId 会话ID
     * @param request   请求
     * @param userId    用户ID
     * @param bodyId    身体ID
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void asyncProcessFittingSession(Long sessionId, DynamicFittingRequest request, Long userId,
                                           Long bodyId) {
        log.info("开始后台分析会话: {}", sessionId);
        try {
            // 前置抓取 Session 数据，避免后续因 getReferenceById 污染 Hibernate 一级缓存产生代理对象
            DynamicFittingSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BizException("找不到对应的会话记录: " + sessionId));

            // 2. 处理各个视角的数据
            Map<String, Object> overallSummary = new java.util.HashMap<>();

            for (DynamicFittingRequest.ViewDetailsDTO viewDetailsDTO : request.getViews()) {
                String viewType = viewDetailsDTO.getViewType() != null ? viewDetailsDTO.getViewType().toLowerCase() : "unknown";
                Map<String, Object> summary = viewDetailsDTO.getSummary() != null ? viewDetailsDTO.getSummary() : new HashMap<>();

                // 2.1 保存 MotionData
                DynamicFittingMotionData motionData = new DynamicFittingMotionData();
                motionData.setSession(session);
                motionData.setViewType(viewType);
                motionData.setDataJson(JSON.toJSONString(viewDetailsDTO.getFrameData()));
                motionData.setTriggerMetricsJson(JSON.toJSONString(viewDetailsDTO.getTriggerMetrics()));

                int frameCount = summary.containsKey("frameCount") ? Integer.parseInt(summary.get("frameCount").toString()) : 0;
                motionData.setFrameCount(frameCount);
                motionDataRepository.save(motionData);

                // 2.2 保存 Snapshots (Base64 -> COS)
                if (viewDetailsDTO.getSnapshots() != null) {
                    for (Map.Entry<String, String> snapEntry : viewDetailsDTO.getSnapshots().entrySet()) {
                        String tag = snapEntry.getKey();
                        String base64 = snapEntry.getValue();

                        // 过滤掉 null 或空字符串的 base64，避免保存无效记录
                        if (!StringUtils.hasText(base64)) {
                            continue;
                        }

                        String imageUrl = uploadBase64Image(base64, userId, viewType, tag);
                        if (!StringUtils.hasText(imageUrl)) {
                            continue;
                        }

                        DynamicFittingSnapshot snapshot = new DynamicFittingSnapshot();
                        snapshot.setSession(session);
                        snapshot.setViewType(viewType);
                        snapshot.setTag(tag);
                        snapshot.setImageUrl(imageUrl);
                        // 尝试从 triggerMetrics 中获取对应数值
                        if (viewDetailsDTO.getTriggerMetrics() != null
                                && viewDetailsDTO.getTriggerMetrics().containsKey(tag + "Val")) {
                            Object val = viewDetailsDTO.getTriggerMetrics().get(tag + "Val");
                            if (val instanceof Number) {
                                snapshot.setFrameVal(BigDecimal.valueOf(((Number) val).doubleValue()));
                            }
                        }
                        snapshotRepository.save(snapshot);
                    }
                }

                // 汇总 summary
                // P11 修复：在后端根据 frameData 计算稳定性指标 (StdDev, CV, Variance)
                Map<String, Object> computedStats = calculateStabilityStats(viewDetailsDTO.getFrameData());
                summary.putAll(computedStats);

                overallSummary.put(viewType, summary);
            }

            // 新增: 融合计算踩踏拟合数据 (Pedaling Simulation)
            Map<String, Object> pedalingResult = pedalingSimulationAlgorithm.analyze(request.getViews());
            if (pedalingResult != null && !pedalingResult.isEmpty()) {
                overallSummary.put("pedalingData", pedalingResult);
            }

            // 3. 生成诊断结果 (Result)
            DynamicFittingResultVO analysisResult = fittingAlgorithm.analyze(session.getUserBike().getBikeModel().getBikeType(),
                    overallSummary);

            // 提取稳定性指标 (Standard Deviation / CV)
            Map<String, Map<String, Object>> stabilitySummary = new HashMap<>();
            overallSummary.forEach((view, metrics) -> {
                if (metrics instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metricMap = (Map<String, Object>) metrics;
                    Map<String, Object> stability = new HashMap<>();
                    metricMap.forEach((k, v) -> {
                        if (k.endsWith("StdDev") || k.endsWith("CV") || k.endsWith("Variance")) {
                            stability.put(k, v);
                        }
                    });
                    if (!stability.isEmpty()) {
                        stabilitySummary.put(view, stability);
                    }
                }
            });

            DynamicFittingResult result = new DynamicFittingResult();
            result.setSession(session);
            result.setUserBody(userBodyRepository.getReferenceById(bodyId));
            result.setMatchScore(analysisResult.getMatchScore());
            result.setSummaryJson(JSON.toJSONString(overallSummary));
            result.setStabilityJson(JSON.toJSONString(stabilitySummary));
            result.setAdviceJson(JSON.toJSONString(analysisResult.getAdvices()));
            result.setStandardJson(JSON.toJSONString(analysisResult.getStandardRanges()));
            resultRepository.save(result);

            // 4. 更新状态为已完成
            session.setStatus(FittingStatus.COMPLETED.getCode());
            sessionRepository.save(session);
            log.info("会话分析完成: {}", sessionId);

        } catch (Exception e) {
            log.error("会话后台分析失败: {}", sessionId, e);
            sessionRepository.findById(sessionId).ifPresent(s -> {
                s.setStatus(FittingStatus.FAILED.getCode());
                sessionRepository.save(s);
            });
        }
    }

    /**
     * 根据用户和车辆查询采集列表
     *
     * @param userId   用户ID
     * @param bikeId   车辆ID
     * @param pageable 分页参数
     */
    public Page<DynamicFittingSessionVO> getSessionsByUser(Long userId, Long bikeId, Pageable pageable) {
        Page<DynamicFittingSession> sessions;
        if (bikeId != null) {
            sessions = sessionRepository.findByUser_IdAndUserBike_IdAndIsDeleted(userId, bikeId, 0, pageable);
        } else {
            sessions = sessionRepository.findByUser_IdAndIsDeleted(userId, 0, pageable);
        }
        return sessions.map(this::convertToVO);
    }

    /**
     * 获取采集会话详情
     *
     * @param sessionId 会话ID
     * @return 会话详情
     */
    public Map<String, Object> getSessionDetail(Long sessionId) {
        DynamicFittingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        Map<String, Object> detail = new HashMap<>();
        detail.put("sessionVO", convertToVO(session));
        return detail;
    }

    /**
     * 将会话转换为VO对象
     *
     * @param session 会话对象
     * @return SessionVO对象
     */
    private DynamicFittingSessionVO convertToVO(DynamicFittingSession session) {
        DynamicFittingSessionVO vo = new DynamicFittingSessionVO();

        // 拷贝基本属性 (id, status, sessionTime, deviceInfo 等)
        BeanUtils.copyProperties(session, vo);

        // 处理关联的单车信息 (防止懒加载异常并提取展示所需字段)
        if (session.getUserBike() != null) {
            vo.setBikeId(session.getUserBike().getId());
            vo.setRemark(session.getUserBike().getRemark());
            // 重要：直接把名字带出去，这样前端历史记录列表就不用再查单车接口了
            try {
                vo.setBikeName(
                        session.getUserBike().getBikeModel() != null ? session.getUserBike().getBikeModel().getModel()
                                : "未知车型");
            } catch (Exception e) {
                log.warn("获取会话 {} 关联的BikeModel失败, userBikeId={}", session.getId(), session.getUserBike().getId());
                vo.setBikeName("未知车型");
            }
        }

        // 处理关联的身体信息
        if (session.getUserBody() != null) {
            vo.setBodyId(session.getUserBody().getId());
        }

        if (session.getPainLevels() != null) {
            List<String> painKeys = JSON.parseArray(session.getPainLevels(), String.class);
            vo.setPainLevels(painKeys);
            // 转换为后端定义的痛点描述
            vo.setPainLabels(painKeys.stream()
                    .map(RidingPainType::getDescriptionByCode)
                    .collect(Collectors.toList()));
        }

        // 提取已完成视角的总结与诊断摘要 (Summary Projection)
        if (session.getStatus() != null && session.getStatus().equals(FittingStatus.COMPLETED.getCode())) {
            resultRepository.findBySession_Id(session.getId()).ifPresent(result -> {
                vo.setMatchScore(result.getMatchScore());

                // 提取精简卡片建议
                if (org.springframework.util.StringUtils.hasText(result.getAdviceJson())) {
                    try {
                        List<DynamicFittingAdviceVO> advices = JSON.parseArray(result.getAdviceJson(), DynamicFittingAdviceVO.class);
                        if (advices != null) {
                            vo.setEquipmentSuggestions(advices.stream()
                                    .map(a -> a.getAdviceContent() != null ? a.getAdviceContent() : a.getCode())
                                    .distinct()
                                    .collect(Collectors.toList()));
                        }
                    } catch (Exception e) {
                        log.warn("解析会话 {} 的 adviceJson 失败", session.getId());
                    }
                }

                if (result.getSummaryJson() != null) {
                    try {
                        Map<String, Object> summary = JSON.parseObject(result.getSummaryJson());
                        vo.setViewTypes(String.join(",", summary.keySet()));
                    } catch (Exception e) {
                        log.warn("解析会话 {} 的 summaryJson 失败", session.getId());
                    }
                }
            });
        }

        return vo;
    }

    /**
     * 获取详细诊断报告 (扁平化结构供 ReportView 展示)
     *
     * @param sessionId 会话ID
     * @return 诊断报告VO对象
     */
    public DynamicFittingReportVO getFittingReport(Long sessionId) {
        DynamicFittingResult result = resultRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new BizException("报告尚未生成或不存在"));
        DynamicFittingSession session = result.getSession();

        DynamicFittingReportVO reportVO = new DynamicFittingReportVO();
        BeanUtils.copyProperties(result, reportVO);

        // 基本元数据
        reportVO.setViewsCount((int) motionDataRepository.countBySession_Id(sessionId));
        reportVO.setBikeId(session.getUserBike() != null ? session.getUserBike().getId() : null);
        reportVO.setBikeName(session.getUserBike() != null && session.getUserBike().getBikeModel() != null
                ? session.getUserBike().getBikeModel().getModel()
                : "未知车型");
        reportVO.setMatchLevel(getMatchLevel(result.getMatchScore()));

        // 简评处理: 汇总所有建议并生成人类可读的总结
        List<DynamicFittingAdviceVO> advices = JSON.parseArray(result.getAdviceJson(), DynamicFittingAdviceVO.class);
        reportVO.setSummary(getSummaryText(result, advices));

        // 解析指标数据 (扁平化)
        Map<String, Map<String, Object>> overallSummary = JSON.parseObject(result.getSummaryJson(),
                new TypeReference<>() {});

        Map<String, String> standardRanges = null;
        if (StringUtils.hasText(result.getStandardJson())) {
            try {
                standardRanges = JSON.parseObject(result.getStandardJson(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("解析会话 {} 的标准范围失败", sessionId);
            }
        }
        reportVO.setStandardRanges(standardRanges);

        if (overallSummary != null) {
            reportVO.setSideData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO(overallSummary.get("side"), advices, standardRanges)));
            reportVO.setFrontData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO(overallSummary.get("front"), advices, standardRanges)));
            reportVO.setBackData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO(overallSummary.get("back"), advices, standardRanges)));

            // 提取被合成好的 PedalingData 数据并装配给前端
            if (overallSummary.containsKey("pedalingData")) {
                Object pMap = overallSummary.get("pedalingData");
                if (pMap != null) {
                    DynamicFittingReportVO.PedalingData pData = JSON.parseObject(JSON.toJSONString(pMap), DynamicFittingReportVO.PedalingData.class);
                    reportVO.setPedalingData(pData);
                }
            }
        }

        // 器材/体能调整建议 (汇总所有建议，不只是 adjust)
        if (advices != null) {
            reportVO.setEquipmentSuggestions(advices.stream()
                    .map(a -> a.getAdviceContent() != null ? a.getAdviceContent() : a.getCode())
                    .distinct()
                    .collect(Collectors.toList()));
        } else {
            reportVO.setEquipmentSuggestions(new ArrayList<>());
        }

        return reportVO;
    }

    /**
     * 基于权威生物力学文献的理想值范围（兜底用）
     * 参考来源：
     * - Holmes, Pruitt & Whalen (1994) "Lower extremity overuse in bicycling", Clin Sports Med
     * - Bini, Hume & Croft (2011) "Effects of bicycle saddle height on knee injury risk", Sports Med
     * - Silberman et al. (2005) "Road bicycle fit", Clin J Sport Med
     * - Retül / GebioMized / Shimano Bike Fitting System 主流平台参考范围
     */
    private static final Map<String, String> LITERATURE_IDEAL_RANGES = new HashMap<>();
    static {
        // 侧面视角
        LITERATURE_IDEAL_RANGES.put("avgTorso", "40.0-50.0");
        LITERATURE_IDEAL_RANGES.put("minHip", "40.0-55.0");
        LITERATURE_IDEAL_RANGES.put("maxHip", "70.0-80.0");
        LITERATURE_IDEAL_RANGES.put("kopsOffset", "-10.0-10.0");
        LITERATURE_IDEAL_RANGES.put("cadenceRpm", "80.0-100.0");
        LITERATURE_IDEAL_RANGES.put("ankleAtBdc", "10.0-25.0");
        LITERATURE_IDEAL_RANGES.put("ankleY", "0.0-15.0");
        LITERATURE_IDEAL_RANGES.put("maxKnee", "140.0-150.0");
        LITERATURE_IDEAL_RANGES.put("maxShoulder", "80.0-95.0");
        // 正面视角
        LITERATURE_IDEAL_RANGES.put("maxWobble", "0.0-15.0");
        LITERATURE_IDEAL_RANGES.put("maxTilt", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("maxElbowFlare", "0.0-15.0");
        LITERATURE_IDEAL_RANGES.put("maxShoulderTilt", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("maxHipTilt", "0.0-5.0");
        // 背面视角
        LITERATURE_IDEAL_RANGES.put("maxPelvisSway", "0.0-10.0");
        LITERATURE_IDEAL_RANGES.put("maxSpineTilt", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("spineRockingVar", "0.0-3.0");
        LITERATURE_IDEAL_RANGES.put("maxHipDrop", "0.0-8.0");
    }

    /**
     * 获取理想范围（优先级：数据库标准 > 文献参考值 > "--"）
     */
    private String resolveIdealRange(String metricKey, DynamicFittingAdviceVO advice, Map<String, String> standardRanges) {
        if (advice != null && advice.getRange() != null) {
            return advice.getRange();
        }
        if (standardRanges != null && standardRanges.containsKey(metricKey)) {
            return standardRanges.get(metricKey);
        }
        return LITERATURE_IDEAL_RANGES.getOrDefault(metricKey, "--");
    }

    /**
     * 生成骑车人自身动作纠正建议（基于指标偏差）
     * 参考主流 Bike Fitting 平台（Retul, GebioMized）的 rider education 思路
     */
    private String getRiderAdvice(String metricKey, double currentVal, String idealRange) {
        if (idealRange.equals("--")) return null;
        try {
            String[] parts = idealRange.split("-");
            double min = Double.parseDouble(parts[0].trim());
            double max = Double.parseDouble(parts[1].trim());
            if (currentVal >= min && currentVal <= max) return null; // 在理想范围内，无需纠正

            boolean isLow = currentVal < min;

            switch (metricKey) {
                case "avgTorso":
                    return isLow
                        ? "骑行时尝试微微抬高上半身，收紧核心肌群（腹横肌），想象「肚脐向脊柱靠拢」。可尝试在骑行时每5分钟提醒自己「挺胸收腹」，避免塌腰。"
                        : "尝试降低躯干，但需确保核心稳定。练习：下车后做猫牛式（Cat-Cow）拉伸，增强胸椎灵活性，让趴低时不会耸肩。";
                case "minHip":
                    return isLow
                        ? "上死点时髋角过小（大腿贴近腹部），建议在踩踏顶部主动「提膝」而非挤压腹部，同时锻炼髋屈肌灵活性。"
                        : "上死点髋角偏大，通常意味着坐垫过高。除调整车辆外，可以尝试单腿踩踏训练（Single Leg Drill）来改善髋关节在顶部的控制力。";
                case "maxHip":
                    return isLow
                        ? "踩踏底部髋角偏小，说明伸展受限。建议在骑行前做髋屈肌拉伸（如弓步髋屈肌拉伸），改善髋关节活动度。"
                        : "髋角偏大，注意踩踏时不要过度下沉骨盆，保持骨盆水平稳定。";
                case "maxKnee":
                    return isLow
                        ? "膝部伸展不足，除了车辆调整外，建议做股四头肌和腘绳肌的动态拉伸（如Leg Swings），并注意踩踏时尽量「画圆」而非「踩踏」。"
                        : "膝盖过度伸展（超伸），存在膝关节后侧韧带损伤风险。骑车时注意保持膝盖微曲，避免完全锁死。可以练习高踏频（90-100 RPM）来改善踩踏平顺度。";
                case "maxShoulder":
                    return isLow
                        ? "肩角偏小，说明上肢过于蜷缩。建议在骑行时保持肘部微曲、肩胛骨微微后收下沉，避免耸肩。"
                        : "肩角偏大，关注是否过度伸展手臂导致重心前移。建议保持肘部弯曲约15-20度，用手臂肌肉吸收震动。";
                case "kopsOffset":
                    return isLow
                        ? "膝盖前移量不足，踩踏时膝部在踏板轴后方。可以尝试在踩踏时主动让膝盖向前「追踪」脚尖方向，感受股四头肌更早介入发力。"
                        : "膝盖前移量过大，增加髌骨压力。注意踩踏时膝盖不要过度超过脚尖，感受臀大肌主导发力。";
                case "cadenceRpm":
                    return isLow
                        ? "踏频偏低（大齿比踩踏），长期如此对膝关节压力较大。建议切换到更轻的齿比，目标踏频80-90 RPM，练习「画圆」式的平顺踩踏。"
                        : "踏频偏高，可能导致身体在车座上跳动。建议配合核心收紧，并且适当增加齿比，找到能稳定骨盆的节奏。";
                case "ankleAtBdc":
                    return isLow
                        ? "下死点踝角偏小（足尖下垂），说明踩踏时过度「垫脚尖」。建议练习在踩踏底部时微微勾起脚尖（dorsiflexion），想象用脚后跟「刮」过底部。"
                        : "踝角过大，可能与锁片位置有关。踩踏时注意保持脚掌平放，不要过度勾脚尖。";
                case "ankleY":
                    return isLow
                        ? "脚踝垂直轨迹较低，可能导致踩踏不连贯。可以练习单腿踩踏训练（Single Leg Drill），专注于脚踝的平滑过渡。"
                        : "脚踝波动过大，可能踩踏时不稳定。建议收紧核心，感受脚掌始终与踏板保持接触，避免「抖动式」踩踏。";
                case "maxWobble":
                    return isLow
                        ? null
                        : "膝盖横向偏移过大，会损耗功率并增加髂胫束（ITB）损伤风险。建议练习：骑行时有意识地保持膝盖「向上、向下」运动（平行于车架），避免膝盖外翻或内扣。可以多做臀中肌激活训练（如蚌式开合、侧卧抬腿）。";
                case "maxTilt":
                    return isLow
                        ? null
                        : "身体侧倾过大，说明核心稳定性不足。建议加强核心抗旋转训练（如Pallof Press、侧平板支撑），骑行时注意骨盆保持水平。";
                case "maxElbowFlare":
                    return isLow
                        ? null
                        : "手肘外张过大，增加风阻且易导致肩部疲劳。骑行时注意保持肘部「内收」（肘部靠近身体，而非向外张开），感受前臂自然下垂。";
                case "maxShoulderTilt":
                    return isLow
                        ? null
                        : "高低肩偏角明显，可能由单侧肌力不均衡或脊柱侧弯趋势引起。建议：加强弱侧斜方肌中下束训练，日常注意左右对称发力。";
                case "maxHipTilt":
                    return isLow
                        ? null
                        : "骨盆侧倾过大，通常暗示坐垫过高或双腿发力不均衡。建议：练习单腿踩踏，并加强臀中肌和核心的抗侧倾能力。";
                case "maxPelvisSway":
                    return isLow
                        ? null
                        : "骨盆横摆位移过大，说明核心稳定性不足或坐垫过高。建议加强核心训练（平板支撑、死虫式），并尝试在骑行时保持骨盆「坐稳」的感觉。";
                case "maxSpineTilt":
                    return isLow
                        ? null
                        : "脊柱侧向偏移明显，可能存在结构性或功能性脊柱侧弯趋势。建议咨询物理治疗师，同时加强核心肌群对称性训练。";
                case "spineRockingVar":
                    return isLow
                        ? null
                        : "脊柱侧偏波动大，说明踩踏时身体不稳定。建议练习稳定踏频下的匀速踩踏，避免身体左右晃动。可以尝试在骑行台上闭眼骑行（安全环境），训练本体感觉。";
                case "maxHipDrop":
                    return isLow
                        ? null
                        : "骨盆下坠明显，说明踩踏时一侧髋关节支撑不足。建议加强弱侧臀中肌力量（如单腿站立、侧卧抬腿），并注意踩踏时保持骨盆水平。";
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将指标数据转换为VO对象
     *
     * @param metrics 指标数据
     * @param advices 诊断建议
     * @return 指标VO对象
     */
    private List<DynamicFittingMetricVO> mapToMetricVO(Map<String, Object> metrics,
                                                       List<DynamicFittingAdviceVO> advices,
                                                       Map<String, String> standardRanges) {
        if (metrics == null)
            return Collections.emptyList();

        List<DynamicFittingMetricVO> vos = new ArrayList<>();
        metrics.forEach((key, val) -> {
            if (key.equals("maxHipDrop_L") || key.equals("maxHipDrop_R")) {
                return; // Skip individual processing, will combine below
            }
            
            if (key.equals("avgTorso") || key.equals("minHip") || key.equals("maxSpineTilt") || key.equals("maxPelvisSway")
                    || key.equals("kopsOffset") || key.equals("cadenceRpm") || key.equals("ankleAtBdc") || key.equals("ankleY") || key.equals("maxHip")
                    || key.equals("maxKnee") || key.equals("maxShoulder") || key.equals("maxWobble") || key.equals("maxTilt") || key.equals("maxElbowFlare")
                    || key.equals("maxShoulderTilt") || key.equals("spineRockingVar")
                    || key.equals("maxHipTilt")) {
                
                DynamicFittingAdviceVO advice = advices.stream()
                        .filter(a -> a.getMetric().equalsIgnoreCase(key))
                        .findFirst().orElse(null);

                String range = resolveIdealRange(key, advice, standardRanges);
                double currentVal = convertToDouble(val);
                String riderAdvice = getRiderAdvice(key, currentVal, range);

                vos.add(DynamicFittingMetricVO.builder()
                        .label(getMetricLabel(key))
                        .currentValue(String.format("%.1f", currentVal))
                        .status(advice != null ? "ADJUST" : "OK")
                        .idealRange(range)
                        .advice(advice != null ? (advice.getAdviceContent() != null ? advice.getAdviceContent() : advice.getCode()) : "处于理想范围")
                        .riderAdvice(riderAdvice)
                        .build());
            }
        });

        // 合并左右骨盆下坠 (Hip Drop) 到一张卡片
        if (metrics.containsKey("maxHipDrop_L") || metrics.containsKey("maxHipDrop_R")) {
            Double lVal = convertToDouble(metrics.get("maxHipDrop_L"));
            Double rVal = convertToDouble(metrics.get("maxHipDrop_R"));
            if (lVal == null) lVal = 0.0;
            if (rVal == null) rVal = 0.0;

            DynamicFittingAdviceVO adviceL = advices.stream().filter(a -> "maxHipDrop_L".equalsIgnoreCase(a.getMetric())).findFirst().orElse(null);
            DynamicFittingAdviceVO adviceR = advices.stream().filter(a -> "maxHipDrop_R".equalsIgnoreCase(a.getMetric())).findFirst().orElse(null);
            DynamicFittingAdviceVO adviceAsym = advices.stream().filter(a -> "maxHipDrop".equalsIgnoreCase(a.getMetric())).findFirst().orElse(null);

            DynamicFittingAdviceVO finalAdvice = adviceAsym != null ? adviceAsym : (adviceL != null ? adviceL : adviceR);
            String status = finalAdvice != null ? "ADJUST" : "OK";
            String adviceText = finalAdvice != null ? (finalAdvice.getAdviceContent() != null ? finalAdvice.getAdviceContent() : finalAdvice.getCode()) : "根据生物力学模型对您的身体关键特征进行追踪量化。";
            String range = resolveIdealRange("maxHipDrop", finalAdvice, standardRanges);
            double maxDrop = Math.max(lVal, rVal);
            String riderAdvice = getRiderAdvice("maxHipDrop", maxDrop, range);

            vos.add(DynamicFittingMetricVO.builder()
                    .label("左右侧骨盆下坠")
                    .currentValue(String.format("L:%.1f R:%.1f", lVal, rVal))
                    .status(status)
                    .idealRange(range)
                    .advice(adviceText)
                    .riderAdvice(riderAdvice)
                    .build());
        }
        return vos;
    }

    /**
     * 获取指标标签
     *
     * @param key 指标key
     * @return 指标标签
     */
    private String getMetricLabel(String key) {
        Map<String, String> labels = new HashMap<>();
        // 侧面
        labels.put("kneeAngle", "膝角 (伸展度)");
        labels.put("hipAngle", "髋角 (重心枢轴)");
        labels.put("torsoAngle", "躯干夹角");
        labels.put("ankleAngle", "踝角");
        labels.put("elbowAngle", "肘部角度");
        labels.put("ankleAtBdc", "下死点踝角");
        labels.put("cadenceRpm", "实时踏频");
        labels.put("kopsOffset", "膝盖前移量(KOPS)");
        
        // 正面
        labels.put("avgKnee", "双膝平均角度");
        labels.put("kneeVariance", "膝角波动方差");
        labels.put("maxWobble", "膝盖最大横摆");
        labels.put("maxTilt", "身体最大侧倾");
        labels.put("maxElbowFlare", "手肘最大外张");
        labels.put("maxHipTilt", "骨盆最大侧倾");
        labels.put("maxShoulderTilt", "高低肩最大偏角");
        labels.put("hipTilt", "骨盆实时侧倾");
        labels.put("shoulderTilt", "肩膀实时偏角");
        labels.put("rightKneeAngle", "右腿伸展度");
        labels.put("leftKneeAngle", "左腿伸展度");
        labels.put("rightKneeWobble", "右膝横摆偏离量");
        labels.put("leftKneeWobble", "左膝横摆偏离量");
        labels.put("rightElbowFlare", "右肘外张角度");
        labels.put("leftElbowFlare", "左肘外张角度");
        
        // 稳定性与汇总
        labels.put("ankleY", "脚踝垂直波动轨迹");
        labels.put("avgTorso", "平均躯干角度");
        labels.put("minHip", "上死点最小髋角");
        labels.put("maxHip", "曲柄底端最大髋角");
        labels.put("avgKneeLateral", "膝盖横向偏移均值");
        labels.put("maxKnee", "最大膝角 (伸展度)");
        labels.put("maxShoulder", "最大肩角 (前伸量)");

        // 背面
        labels.put("maxPelvisSway", "骨盆横摆位移");
        labels.put("maxSpineTilt", "脊柱侧中心偏移");
        labels.put("spineRockingVar", "脊柱侧偏波动极差");
        labels.put("maxHipDrop_L", "左侧骨盆下坠");
        labels.put("maxHipDrop_R", "右侧骨盆下坠");

        return labels.getOrDefault(key, key);
    }

    /**
     * 获取简要诊断报告
     *
     * @param result  fitting 结果
     * @param advices 建议
     * @return 简要诊断报告
     */
    private String getSummaryText(DynamicFittingResult result, List<DynamicFittingAdviceVO> advices) {
        StringBuilder sb = new StringBuilder();
        double score = result.getMatchScore() != null ? result.getMatchScore().doubleValue() : 0.0;

        // 1. 得分定性评价
        if (score >= 90) sb.append("您的骑行姿态与车型几何高度适配，表现非常出色。");
        else if (score >= 80) sb.append("整体姿态表现良好，但在局部细节上仍有微调空间。");
        else if (score >= 70) sb.append("系统发现若干项关键姿态偏差，建议通过微调来优化您的舒适度。");
        else sb.append("您的生物力学模型显示明显偏移，建议对车辆规格进行大幅调整。");

        // 2. 痛点关联 (解析 Session 中的 painLevels)
        String painJson = result.getSession().getPainLevels();
        if (painJson != null && !painJson.isEmpty()) {
            try {
                List<String> painKeys = JSON.parseArray(painJson, String.class);
                if (painKeys != null && !painKeys.isEmpty()) {
                    List<String> labels = painKeys.stream()
                            .map(RidingPainType::getDescriptionByCode)
                            .collect(Collectors.toList());

                    sb.append(String.format("针对您反馈的 [%s] 等部位，系统已为您识别出专项优化建议：", String.join("、", labels)));
                }
            } catch (Exception ignored) {
            }
        }

        // 3. 建议数量统计
        if (advices != null && !advices.isEmpty()) {
            long adjustCount = advices.size();
            sb.append(String.format("共包含 %d 项关键调整指导。", adjustCount));
        }

        return sb.toString();
    }

    /**
     * 获取评分等级
     *
     * @param score 评分
     * @return 等级
     */
    private String getMatchLevel(BigDecimal score) {
        if (score == null)
            return "未知评分";
        double s = score.doubleValue();
        if (s >= 90)
            return "专业推荐 (PRO)";
        if (s >= 80)
            return "高度适配 (OPTIMAL)";
        if (s >= 70)
            return "基本合格 (BASIC)";
        return "需大幅调整 (RE-FIT)";
    }

    /**
     * 将对象转换为Double
     *
     * @param val 对象
     * @return Double
     */
    private Double convertToDouble(Object val) {
        if (val == null)
            return 0.0;
        if (val instanceof Number)
            return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 获取原始轨迹序列 (用于绘图)
     *
     * @param sessionId 会话ID
     * @param viewType  视图类型
     * @return 轨迹序列
     */
    public DynamicFittingMotionDataVO getMotionData(Long sessionId, String viewType) {
        DynamicFittingMotionData data = motionDataRepository
                .findBySession_IdAndViewType(sessionId, viewType)
                .orElse(null);
        if (data == null) {
            return null;
        }
        DynamicFittingMotionDataVO vo = new DynamicFittingMotionDataVO();
        vo.setId(data.getId());
        vo.setViewType(data.getViewType());
        vo.setDataJson(data.getDataJson()); // 显式映射大字段
        vo.setTriggerMetricsJson(data.getTriggerMetricsJson());
        vo.setFrameCount(data.getFrameCount());
        return vo;
    }

    /**
     * 获取原关键截图
     */
    public List<DynamicFittingSnapshotVO> getSnapshots(Long sessionId, String viewType) {
        List<DynamicFittingSnapshot> snapshots = snapshotRepository.findBySession_IdAndViewType(sessionId, viewType);
        return snapshots.stream().map(snapshot -> {
            DynamicFittingSnapshotVO vo = new DynamicFittingSnapshotVO();
            vo.setId(snapshot.getId());
            vo.setSessionId(sessionId);
            vo.setViewType(snapshot.getViewType());
            vo.setLabel(snapshot.getTag()); // tag -> label
            vo.setUrl(snapshot.getImageUrl()); // imageUrl -> url
            vo.setMetricValue(snapshot.getFrameVal()); // frameVal -> metricValue
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 级联删除会话
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFittingSession(Long sessionId) {
        // 1. 删除 OSS 图片 (此处需要解析 URL 获取 key)
        List<DynamicFittingSnapshot> snapshots = snapshotRepository.findBySession_Id(sessionId);
        for (DynamicFittingSnapshot snapshot : snapshots) {
            try {
                // 简单的 key 提取逻辑 (需根据 CosUtil 实际存储路径调整)
                String url = snapshot.getImageUrl();
                if (org.springframework.util.StringUtils.hasText(url) && url.contains("fitting/")) {
                    String key = url.substring(url.indexOf("fitting/"));
                    cosUtil.delete(key);
                }
            } catch (Exception e) {
                log.error("删除 OSS 图片失败: {}", snapshot.getImageUrl(), e);
            }
        }

        // 2. 删除数据库各表关联记录
        snapshotRepository.deleteAll(snapshots);

        motionDataRepository.findBySession_IdAndViewType(sessionId, "side").ifPresent(motionDataRepository::delete);
        motionDataRepository.findBySession_IdAndViewType(sessionId, "front").ifPresent(motionDataRepository::delete);
        motionDataRepository.findBySession_IdAndViewType(sessionId, "back").ifPresent(motionDataRepository::delete);

        resultRepository.findBySession_Id(sessionId).ifPresent(resultRepository::delete);

        sessionRepository.deleteById(sessionId);
    }

    /**
     * 计算时序数据的稳定性指标 (均值、标准差、变异系数)
     * P11 修复核心逻辑
     *
     * @param frameData 帧数据列表
     * @return 统计指标 Map
     */
    private Map<String, Object> calculateStabilityStats(List<Map<String, Object>> frameData) {
        if (frameData == null || frameData.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 采集各个指标的时序序列
        Map<String, List<Double>> seriesMap = new HashMap<>();
        for (Map<String, Object> frame : frameData) {
            Object metricsObj = frame.get("metrics");
            if (metricsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = (Map<String, Object>) metricsObj;
                metrics.forEach((k, v) -> {
                    Double val = convertToDouble(v);
                    if (val != null) {
                        seriesMap.computeIfAbsent(k, key -> new ArrayList<>()).add(val);
                    }
                });
            }
        }

        // 2. 计算统计学指标
        Map<String, Object> stats = new HashMap<>();
        seriesMap.forEach((metric, values) -> {
            int n = values.size();
            if (n < 5) return; // 数据点太少不计算统计性指标

            double sum = 0;
            for (Double v : values) sum += v;
            double mean = sum / n;

            double sumSqDiff = 0;
            for (Double v : values) {
                sumSqDiff += Math.pow(v - mean, 2);
            }
            double variance = sumSqDiff / n;
            double stdDev = Math.sqrt(variance);
            // CV (变异系数) = 标准差 / 均值 * 100%
            double cv = (Math.abs(mean) > 0.001) ? (stdDev / Math.abs(mean)) * 100 : 0;

            // 保留两位小数
            stats.put(metric + "Mean", Math.round(mean * 100.0) / 100.0);
            stats.put(metric + "StdDev", Math.round(stdDev * 100.0) / 100.0);
            stats.put(metric + "CV", Math.round(cv * 100.0) / 100.0);
            stats.put(metric + "Variance", Math.round(variance * 100.0) / 100.0);
        });

        return stats;
    }

    private String uploadBase64Image(String base64, Long userId, String viewType, String tag) {
        try {
            // 去掉 data:image/png;base64, 前缀
            String data = base64;
            if (base64.contains(",")) {
                data = base64.substring(base64.indexOf(",") + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(data);

//            String middlePath = "/" + userId + "/fitting/";
//            FileUploadResult fileUploadResult = fileUploadUtil.uploadPic(middlePath, bytes);
//            return fileUploadResult.getUrl();

             String key = String.format("/%d/fitting/%s_%s_%s.png",
                     userId, viewType, tag, UUID.randomUUID().toString().substring(0, 8));

             return cosUtil.upload(new ByteArrayInputStream(bytes), bytes.length, key);
        } catch (Exception e) {
            log.error("上传 Base64 图片失败: {} - {}", viewType, tag, e);
            return "";
        }
    }

    /**
     * 获取用户的动态Fitting报告总数
     *
     * @param userId 用户ID
     * @return 报告数量
     */
    public long countByUserId(Long userId) {
        return resultRepository.countByUserId(userId);
    }
}
