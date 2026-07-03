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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern RANGE_NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Set<String> CYCLIC_SIDE_METRICS = Set.of("kneeAngle", "hipAngle", "ankleAngle");
    private static final Set<String> POSTURE_STABILITY_METRICS = Set.of(
            "torsoAngle",
            "leftKneeWobble", "rightKneeWobble",
            "leftElbowFlare", "rightElbowFlare",
            "shoulderTilt", "hipTilt",
            "pelvisSway", "spineTilt",
            "leftHipDrop", "rightHipDrop"
    );

    /**
     * 属于动作/习惯指导类的指标：不应列入"器材更换/调整建议"，只在"骑车习惯 & 动作纠正要点"中体现。
     */
    private static final Set<String> ACTION_ONLY_METRICS = Set.of(
            "kneeAnglePhaseStdDev",
            "hipAnglePhaseStdDev",
            "ankleAnglePhaseStdDev",
            "cadenceRpm",
            "spineRockingVar"
    );

    /**
     * 器材部位中文映射，用于生成合并后的器材建议标题。
     * 未识别部位归入「综合调整」，避免所有建议都显示无意义的"调整建议"。
     */
    private static final Map<String, String> EQUIPMENT_PART_LABEL = Map.of(
            "cleat", "锁片调整",
            "seat", "座垫调整",
            "handlebar", "车把/把立调整",
            "crank", "综合调整",
            "shoe", "锁鞋/鞋垫",
            "saddle", "座垫调整",
            "stem", "车把/把立调整",
            "fitter", "综合调整"
    );

    /**
     * 疼痛部位 → 关联指标映射表
     * 用于报告中的疼痛关联分析，将用户反馈的疼痛部位自动关联到相关生物力学指标
     */
    private static final Map<String, Set<String>> PAIN_METRIC_MAP = new HashMap<>();
    static {
        PAIN_METRIC_MAP.put("knee_anterior", Set.of("maxKnee", "kopsOffset"));
        PAIN_METRIC_MAP.put("knee_posterior", Set.of("maxKnee"));
        PAIN_METRIC_MAP.put("knee_medial", Set.of("leftKneeWobble", "rightKneeWobble", "maxWobble"));
        PAIN_METRIC_MAP.put("knee_lateral", Set.of("leftKneeWobble", "rightKneeWobble", "maxWobble"));
        PAIN_METRIC_MAP.put("neck_pain", Set.of("avgTorso"));
        PAIN_METRIC_MAP.put("shoulder_pain", Set.of("maxShoulderTilt", "maxElbowFlare"));
        PAIN_METRIC_MAP.put("hand_numbness", Set.of("avgTorso", "maxElbowFlare"));
        PAIN_METRIC_MAP.put("lower_back", Set.of("avgTorso", "pelvisSway", "maxPelvisSway"));
        PAIN_METRIC_MAP.put("perineum_pain", Set.of("avgTorso", "kopsOffset"));
        PAIN_METRIC_MAP.put("foot_numbness", Set.of("ankleAtBdc"));
        PAIN_METRIC_MAP.put("achilles_pain", Set.of("ankleAtBdc"));
    }

    /** 疼痛部位中文描述 → 调车建议的映射 */
    private static final Map<String, String> PAIN_ADJUSTMENT_HINTS = new HashMap<>();
    static {
        PAIN_ADJUSTMENT_HINTS.put("knee_anterior", "建议优先调整座高和坐垫前后位置");
        PAIN_ADJUSTMENT_HINTS.put("knee_posterior", "建议优先检查座高是否过高，关注膝盖后方不适");
        PAIN_ADJUSTMENT_HINTS.put("knee_medial", "建议检查锁片角度和鞋垫支撑，加强髋部控制训练");
        PAIN_ADJUSTMENT_HINTS.put("knee_lateral", "建议检查锁片角度和鞋垫支撑，加强臀中肌力量");
        PAIN_ADJUSTMENT_HINTS.put("neck_pain", "建议检查把立高度和长度，避免过度抬头或耸肩");
        PAIN_ADJUSTMENT_HINTS.put("shoulder_pain", "建议检查车把宽度和握把姿势，放松上半身");
        PAIN_ADJUSTMENT_HINTS.put("hand_numbness", "建议调整坐垫前后位置和把立高度，减少手部压力");
        PAIN_ADJUSTMENT_HINTS.put("lower_back", "建议检查躯干俯角和骨盆稳定性，加强核心力量");
        PAIN_ADJUSTMENT_HINTS.put("perineum_pain", "建议调整坐垫角度和前后位置，检查坐垫选择是否合适");
        PAIN_ADJUSTMENT_HINTS.put("foot_numbness", "建议检查锁片位置和鞋底支撑，调整踩踏姿势");
        PAIN_ADJUSTMENT_HINTS.put("achilles_pain", "建议检查锁片前后位置和坐垫高度，减少跟腱拉伸");
    }

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
     * 注意：本方法上 @Async 和 @Transactional 的组合在 Spring 中工作正常：
     * @Async 将方法调度到异步线程池执行，@Transactional 在异步线程中开启独立事务。
     * 与 saveFittingSession 的事务完全隔离，互不影响。
     * 若异步处理失败，catch 块会独立将 session 状态更新为 FAILED。
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
            sessions = sessionRepository.findByUserIdAndBikeIdAndIsDeletedWithDetails(userId, bikeId, 0, pageable);
        } else {
            sessions = sessionRepository.findByUserIdAndIsDeletedWithDetails(userId, 0, pageable);
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

                // 提取精简卡片建议：与报告页保持一致，合并整理器材调整建议
                if (org.springframework.util.StringUtils.hasText(result.getAdviceJson())) {
                    try {
                        List<DynamicFittingAdviceVO> advices = JSON.parseArray(result.getAdviceJson(), DynamicFittingAdviceVO.class);
                        vo.setEquipmentSuggestions(buildEquipmentSuggestions(advices));
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

        // 疼痛关联分析：提取用户反馈的疼痛部位，生成分析文本
        Set<String> painMetricKeys = new HashSet<>();
        String painRaw = session.getPainLevels();
        if (StringUtils.hasText(painRaw)) {
            try {
                List<String> painKeys = JSON.parseArray(painRaw, String.class);
                if (painKeys != null && !painKeys.isEmpty()) {
                    // 收集所有疼痛部位关联的指标 key
                    for (String painKey : painKeys) {
                        Set<String> related = PAIN_METRIC_MAP.get(painKey);
                        if (related != null) {
                            painMetricKeys.addAll(related);
                        }
                    }
                    // 生成疼痛分析文本
                    reportVO.setPainAnalysis(generatePainAnalysis(painKeys, overallSummary, standardRanges));
                    reportVO.setPainLabels(painKeys.stream()
                            .map(RidingPainType::getDescriptionByCode)
                            .collect(Collectors.toList()));
                }
            } catch (Exception e) {
                log.warn("解析会话 {} 的疼痛数据失败", sessionId);
            }
        }

        // 计算检测覆盖率
        Map<String, String> finalStandardRanges = standardRanges;
        int totalStandards = finalStandardRanges != null ? finalStandardRanges.size() : 0;
        int detectedCount = 0;
        if (overallSummary != null) {
            for (Map<String, Object> viewMetrics : overallSummary.values()) {
                if (viewMetrics instanceof Map && !"pedalingData".equals(viewMetrics)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> vm = (Map<String, Object>) viewMetrics;
                    detectedCount += (int) vm.keySet().stream()
                            .filter(k -> finalStandardRanges != null && finalStandardRanges.containsKey(k))
                            .count();
                }
            }
        }
        reportVO.setDetectedMetricsCount(detectedCount);
        reportVO.setTotalStandardMetricsCount(totalStandards);

        if (overallSummary != null) {
            reportVO.setSideData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO("side", overallSummary.get("side"), advices, standardRanges, painMetricKeys)));
            reportVO.setFrontData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO("front", overallSummary.get("front"), advices, standardRanges, painMetricKeys)));
            reportVO.setBackData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO("back", overallSummary.get("back"), advices, standardRanges, painMetricKeys)));

            // 提取被合成好的 PedalingData 数据并装配给前端
            if (overallSummary.containsKey("pedalingData")) {
                Object pMap = overallSummary.get("pedalingData");
                if (pMap != null) {
                    DynamicFittingReportVO.PedalingData pData = JSON.parseObject(JSON.toJSONString(pMap), DynamicFittingReportVO.PedalingData.class);
                    reportVO.setPedalingData(pData);
                }
            }
        }

        // 器材调整建议：仅保留真正需要调整器材的部位，按锁片/座垫/车把合并整理，避免与动作指导混淆
        reportVO.setEquipmentSuggestions(buildEquipmentSuggestions(advices));

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
        LITERATURE_IDEAL_RANGES.put("kopsOffset", "-15.0-15.0");
        LITERATURE_IDEAL_RANGES.put("cadenceRpm", "70.0-110.0");
        LITERATURE_IDEAL_RANGES.put("ankleAtBdc", "0.0-30.0");
        LITERATURE_IDEAL_RANGES.put("ankleY", "0.0-15.0");
        LITERATURE_IDEAL_RANGES.put("maxKnee", "145.0-150.0");
        LITERATURE_IDEAL_RANGES.put("maxShoulder", "80.0-95.0");
        // 正面视角
        LITERATURE_IDEAL_RANGES.put("maxWobble", "0.0-15.0");
        LITERATURE_IDEAL_RANGES.put("maxTilt", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("maxElbowFlare", "0.0-15.0");
        LITERATURE_IDEAL_RANGES.put("maxShoulderTilt", "0.0-7.0");
        LITERATURE_IDEAL_RANGES.put("maxHipTilt", "0.0-7.0");
        // 背面视角
        LITERATURE_IDEAL_RANGES.put("maxPelvisSway", "0.0-10.0");
        LITERATURE_IDEAL_RANGES.put("maxSpineTilt", "0.0-7.0");
        LITERATURE_IDEAL_RANGES.put("spineRockingVar", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("maxHipDrop", "0.0-8.0");
        // 稳定性指标：侧面周期性关节角使用相位一致性，姿态/横摆类指标使用原始标准差。
        LITERATURE_IDEAL_RANGES.put("kneeAnglePhaseStdDev", "0.0-4.0");
        LITERATURE_IDEAL_RANGES.put("hipAnglePhaseStdDev", "0.0-4.0");
        LITERATURE_IDEAL_RANGES.put("ankleAnglePhaseStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("torsoAngleStdDev", "0.0-3.0");
        LITERATURE_IDEAL_RANGES.put("shoulderTiltStdDev", "0.0-3.0");
        LITERATURE_IDEAL_RANGES.put("hipTiltStdDev", "0.0-3.0");
        LITERATURE_IDEAL_RANGES.put("leftKneeWobbleStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("rightKneeWobbleStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("leftElbowFlareStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("rightElbowFlareStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("pelvisSwayStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("spineTiltStdDev", "0.0-3.0");
        LITERATURE_IDEAL_RANGES.put("leftHipDropStdDev", "0.0-5.0");
        LITERATURE_IDEAL_RANGES.put("rightHipDropStdDev", "0.0-5.0");
    }

    private static final Map<String, List<String>> REPORT_METRIC_ORDER = new HashMap<>();
    static {
        REPORT_METRIC_ORDER.put("side", List.of(
                "avgTorso",
                "maxKnee",
                "ankleAtBdc",
                "minHip",
                "cadenceRpm",
                "kopsOffset"
        ));
        REPORT_METRIC_ORDER.put("front", List.of(
                "maxHipTilt",
                "maxShoulderTilt",
                "maxWobble",
                "maxElbowFlare"
        ));
        REPORT_METRIC_ORDER.put("back", List.of(
                "spineRockingVar",
                "maxPelvisSway",
                "maxShoulderTilt",
                "maxSpineTilt",
                "maxHipDrop"
        ));
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
        if (!StringUtils.hasText(idealRange) || idealRange.equals("--")) return null;
        try {
            double[] range = parseRange(idealRange);
            if (range == null) return null;
            double min = range[0];
            double max = range[1];
            if (currentVal >= min && currentVal <= max) return null; // 在理想范围内，无需纠正

            boolean isLow = currentVal < min;

            switch (metricKey) {
                case "avgTorso":
                    return isLow
                        ? "收紧核心，挺胸收腹，避免塌腰。"
                        : "逐步降低姿势前，先加强胸椎灵活性和核心力量。";
                case "minHip":
                    return isLow
                        ? "踏板最高点时有意识提膝，多做髋屈肌拉伸。"
                        : "练习单腿踩踏，感受踩踏顶部的髋关节控制。";
                case "maxHip":
                    return isLow
                        ? "骑行前做髋屈肌和股四头肌拉伸。"
                        : "踏板最低点时保持骨盆水平，收紧臀部发力。";
                case "maxKnee":
                    return isLow
                        ? "练习「画圆」踩踏，避免单纯向下踩。"
                        : "膝盖保持微曲不锁死，用轻齿比练习高踏频。";
                case "maxShoulder":
                    return isLow
                        ? "肘部微曲，肩胛骨后收下沉，避免耸肩。"
                        : "肘部保持微曲，让手臂吸收路面震动。";
                case "kopsOffset":
                    return isLow
                        ? "膝盖追踪脚尖方向，感受大腿前侧发力。"
                        : "膝盖不过度超脚尖，用臀部主导发力。";
                case "cadenceRpm":
                    return isLow
                        ? "换轻齿比，保持踏频 80-90 RPM。"
                        : "收紧核心，增加齿比，保持骨盆稳定。";
                case "ankleAtBdc":
                    return isLow
                        ? "脚掌平稳过渡，避免脚跟过度下坠。"
                        : "减少脚尖下压，脚掌平稳通过最低点。";
                case "ankleY":
                    return isLow
                        ? "练习单腿踩踏，让脚踝平滑过渡。"
                        : "收紧核心，脚掌始终贴合踏板。";
                case "maxWobble":
                    return isLow ? null
                        : "膝盖保持直线运动，做臀中肌激活训练（侧卧抬腿、弹力带螃蟹步）。";
                case "maxTilt":
                    return isLow ? null
                        : "加强核心抗旋转训练，骑行时保持骨盆水平。";
                case "maxElbowFlare":
                    return isLow ? null
                        : "肘部内收靠近身体，前臂自然下垂。";
                case "maxShoulderTilt":
                    return isLow ? null
                        : "加强弱侧肩胛稳定训练，放松握把。";
                case "maxHipTilt":
                    return isLow ? null
                        : "练习单腿踩踏，加强臀中肌和核心抗侧倾训练。";
                case "maxPelvisSway":
                    return isLow ? null
                        : "加强核心训练（平板支撑、死虫式），骑行时稳住骨盆。";
                case "maxSpineTilt":
                    return isLow ? null
                        : "注意左右对称发力，改善脊柱中线偏移。";
                case "spineRockingVar":
                    return isLow ? null
                        : "保持匀速踏频，收紧核心，避免身体摇摆。";
                case "maxHipDrop":
                    return isLow ? null
                        : "加强弱侧臀中肌训练，踩踏时保持骨盆水平。";
                // 稳定性指标
                case "kneeAnglePhaseStdDev":
                    return isLow ? null
                        : "练习「画圆」踩踏和单腿训练，让每圈踩踏更均匀。";
                case "hipAnglePhaseStdDev":
                    return isLow ? null
                        : "保持匀速踏频，配合髋屈肌拉伸。";
                case "ankleAnglePhaseStdDev":
                    return isLow ? null
                        : "用轻齿比练高踏频，保持脚掌稳定接触踏板。";
                case "torsoAngleStdDev":
                    return isLow ? null
                        : "加强核心训练，骑行时保持上半身稳定。";
                case "shoulderTiltStdDev":
                    return isLow ? null
                        : "放松握把，肘部微曲，上半身保持放松。";
                case "hipTiltStdDev":
                    return isLow ? null
                        : "尝试降低座高 2-3mm，加强侧平板支撑。";
                case "pelvisSwayStdDev":
                    return isLow ? null
                        : "练习高踏频稳定训练，加强核心抗旋转。";
                default:
                    // 兜底：尝试匹配带 StdDev/CV/PhaseStdDev 后缀的稳定性指标
                    if (metricKey.endsWith("PhaseStdDev") || metricKey.endsWith("StdDev") || metricKey.endsWith("CV")) {
                        return isLow ? null
                            : "该项稳定性不足，建议通过稳定踏频和核心练习改善。";
                    }
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 构建合并后的器材调整建议。
     * 过滤掉纯动作/习惯指导类指标（如相位一致性、踏频），并按器材部位（锁片/座垫/车把）合并，避免建议过于零散。
     */
    private List<String> buildEquipmentSuggestions(List<DynamicFittingAdviceVO> advices) {
        if (advices == null || advices.isEmpty()) {
            return new ArrayList<>();
        }

        // 识别 part → 中文标签；未识别但 adviceContent 有实质内容的，归入「综合调整】
        Map<String, List<DynamicFittingAdviceVO>> grouped = new LinkedHashMap<>();
        for (DynamicFittingAdviceVO a : advices) {
            if (a.getMetric() != null && ACTION_ONLY_METRICS.contains(a.getMetric())) continue;

            String content = a.getAdviceContent() != null ? a.getAdviceContent() : a.getCode();
            if (!StringUtils.hasText(content)) continue;

            String part = a.getPart();
            String key = EQUIPMENT_PART_LABEL.containsKey(part) ? part : "fitter";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }

        List<String> result = new ArrayList<>();
        // 保持常用部位的固定展示顺序
        for (String part : List.of("cleat", "seat", "saddle", "handlebar", "stem", "shoe", "crank", "fitter")) {
            List<DynamicFittingAdviceVO> list = grouped.get(part);
            if (list == null || list.isEmpty()) continue;

            String combined = list.stream()
                    .map(a -> {
                        String content = a.getAdviceContent() != null ? a.getAdviceContent() : a.getCode();
                        if (!StringUtils.hasText(content)) return null;
                        return content.replaceAll("[。；,]+$", "");
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining("；"));

            if (StringUtils.hasText(combined)) {
                result.add("【" + EQUIPMENT_PART_LABEL.get(part) + "】" + combined + "。");
            }
        }
        return result;
    }

    private double[] parseRange(String rangeText) {
        if (!StringUtils.hasText(rangeText)) return null;
        String compact = rangeText.replace(" ", "");
        Matcher firstMatcher = RANGE_NUMBER_PATTERN.matcher(compact);
        if (!firstMatcher.find()) return null;
        double first = Double.parseDouble(firstMatcher.group());

        int secondStart = firstMatcher.end();
        if (secondStart < compact.length() && compact.charAt(secondStart) == '-') {
            secondStart++;
        }

        Matcher secondMatcher = RANGE_NUMBER_PATTERN.matcher(compact);
        if (!secondMatcher.find(secondStart)) return null;
        double second = Double.parseDouble(secondMatcher.group());

        return new double[]{Math.min(first, second), Math.max(first, second)};
    }

    private double normalizeMetricValueForRange(String metricKey, double value, String rangeText) {
        if (!"ankleAtBdc".equals(metricKey)) return value;
        double[] range = parseRange(rangeText);
        if (range == null) return value;

        // 兼容历史报告：旧采集值是 ∠KNEE-ANKLE-TOE 内角（约 90-120°），
        // 新业务口径是跖屈代理角（约 10-25°）。显示/判定时按标准区间自动换算。
        // 使用严格阈值避免误判：内角低于80度不可行，跖屈代理角高于50度不合理
        if (range[1] <= 45.0 && value > 80.0 && value <= 125.0) {
            return value - 90.0;
        }
        if (range[0] >= 60.0 && value >= -45.0 && value <= 45.0) {
            return value + 90.0;
        }
        return value;
    }

    private String resolveStatusFromRange(double currentVal, String rangeText) {
        double[] range = parseRange(rangeText);
        if (range == null) return "OK";
        return currentVal < range[0] || currentVal > range[1] ? "ADJUST" : "OK";
    }

    private String getRangeDerivedAdvice(String metricKey, double currentVal, String rangeText) {
        double[] range = parseRange(rangeText);
        if (range == null) return "该项超出参考范围，建议复核采集质量并结合实际骑行感受判断。";
        boolean isLow = currentVal < range[0];

        switch (metricKey) {
            case "maxHipTilt":
                return "骨盆侧倾偏大。建议检查：①座高是否过高；②坐垫支撑是否足够。";
            case "ankleAtBdc":
                return isLow
                        ? "脚跟偏下沉。建议检查锁片前后位置、鞋底支撑。"
                        : "脚尖下压偏多。建议检查锁片前后位置、鞋底支撑。";
            case "maxShoulderTilt":
            case "maxTilt":
                return "上身侧倾偏大。建议检查：①摄像头是否水平；②握把压力是否均匀。排除拍摄因素后，再确认核心控制。";
            default:
                return "该项超出参考范围。建议复核拍摄质量，结合骑行实际感受判断。";
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
        return mapToMetricVO(null, metrics, advices, standardRanges, new HashSet<>());
    }

    private List<DynamicFittingMetricVO> mapToMetricVO(String viewType,
                                                       Map<String, Object> metrics,
                                                       List<DynamicFittingAdviceVO> advices,
                                                       Map<String, String> standardRanges,
                                                       Set<String> painMetricKeys) {
        if (metrics == null) {
            return Collections.emptyList();
        }

        List<DynamicFittingAdviceVO> safeAdvices = advices != null ? advices : Collections.emptyList();
        List<DynamicFittingMetricVO> vos = new ArrayList<>();
        for (String key : resolveReportMetricOrder(viewType)) {
            if ("maxHipDrop".equals(key)) {
                addHipDropMetricVO(vos, metrics, safeAdvices, standardRanges, painMetricKeys);
                continue;
            }
            if (!metrics.containsKey(key)) {
                continue;
            }
            addSingleMetricVO(vos, key, metrics.get(key), safeAdvices, standardRanges, painMetricKeys);
        }
        return vos;
    }

    private List<String> resolveReportMetricOrder(String viewType) {
        if (StringUtils.hasText(viewType)) {
            List<String> keys = REPORT_METRIC_ORDER.get(viewType.toLowerCase());
            if (keys != null) {
                return keys;
            }
        }

        LinkedHashSet<String> allKeys = new LinkedHashSet<>();
        REPORT_METRIC_ORDER.values().forEach(allKeys::addAll);
        return new ArrayList<>(allKeys);
    }

    private void addSingleMetricVO(List<DynamicFittingMetricVO> vos,
                                   String key,
                                   Object val,
                                   List<DynamicFittingAdviceVO> advices,
                                   Map<String, String> standardRanges,
                                   Set<String> painMetricKeys) {
        DynamicFittingAdviceVO advice = advices.stream()
                .filter(a -> key.equalsIgnoreCase(a.getMetric()))
                .findFirst().orElse(null);

        String range = resolveIdealRange(key, advice, standardRanges);
        double currentVal = normalizeMetricValueForRange(key, convertToDouble(val), range);
        String status = advice != null ? "ADJUST" : resolveStatusFromRange(currentVal, range);
        String riderAdvice = getRiderAdvice(key, currentVal, range);
        String adviceText = advice != null
                ? (advice.getAdviceContent() != null ? advice.getAdviceContent() : advice.getCode())
                : ("OK".equals(status) ? "处于理想范围" : getRangeDerivedAdvice(key, currentVal, range));

        vos.add(DynamicFittingMetricVO.builder()
                .label(getMetricLabel(key))
                .currentValue(String.format("%.1f", currentVal))
                .status(status)
                .idealRange(range)
                .advice(adviceText)
                .riderAdvice(riderAdvice)
                .painRelated(painMetricKeys != null && painMetricKeys.contains(key))
                .build());
    }

    private void addHipDropMetricVO(List<DynamicFittingMetricVO> vos,
                                    Map<String, Object> metrics,
                                    List<DynamicFittingAdviceVO> advices,
                                    Map<String, String> standardRanges,
                                    Set<String> painMetricKeys) {
        if (!metrics.containsKey("hipDrop_L") && !metrics.containsKey("hipDrop_R")) {
            return;
        }

        Double lVal = convertToDouble(metrics.get("hipDrop_L"));
        Double rVal = convertToDouble(metrics.get("hipDrop_R"));
        if (lVal == null) lVal = 0.0;
        if (rVal == null) rVal = 0.0;

        DynamicFittingAdviceVO adviceL = advices.stream().filter(a -> "hipDrop_L".equalsIgnoreCase(a.getMetric())).findFirst().orElse(null);
        DynamicFittingAdviceVO adviceR = advices.stream().filter(a -> "hipDrop_R".equalsIgnoreCase(a.getMetric())).findFirst().orElse(null);
        // 注意：performBilateralAudit 中不对称告警的 metric 为 baseMetric（"hipDrop"），
        // 而非 "maxHipDrop"。同时检查双侧告警和单侧超限告警。
        DynamicFittingAdviceVO adviceAsym = advices.stream().filter(a -> "hipDrop".equalsIgnoreCase(a.getMetric())).findFirst().orElse(null);

        DynamicFittingAdviceVO finalAdvice = adviceAsym != null ? adviceAsym : (adviceL != null ? adviceL : adviceR);
        // 不对称告警存在时优先使用双侧差值范围，否则使用 maxHipDrop 理想范围
        String range = adviceAsym != null
                ? resolveIdealRange("hipDrop", finalAdvice, standardRanges)
                : resolveIdealRange("maxHipDrop", finalAdvice, standardRanges);
        double maxDrop = Math.max(lVal, rVal);
        String status = finalAdvice != null ? "ADJUST" : resolveStatusFromRange(maxDrop, range);
        String adviceText = finalAdvice != null
                ? (finalAdvice.getAdviceContent() != null ? finalAdvice.getAdviceContent() : finalAdvice.getCode())
                : ("OK".equals(status) ? "处于理想范围"
                : getRangeDerivedAdvice("maxHipDrop", maxDrop, range));
        String riderAdvice = getRiderAdvice("maxHipDrop", maxDrop, range);

        vos.add(DynamicFittingMetricVO.builder()
                .label("左右侧骨盆下坠")
                .currentValue(String.format("L:%.1f R:%.1f", lVal, rVal))
                .status(status)
                .idealRange(range)
                .advice(adviceText)
                .riderAdvice(riderAdvice)
                .painRelated(painMetricKeys != null && painMetricKeys.contains("maxHipDrop"))
                .build());
    }

    /**
     * 生成疼痛关联分析文本
     * 根据用户反馈的疼痛部位，自动关联相关生物力学指标，生成自然语言分析
     *
     * @param painKeys       疼痛部位编码列表
     * @param overallSummary 各视角指标汇总数据
     * @param standardRanges 标准范围映射
     * @return 疼痛关联分析文本
     */
    private String generatePainAnalysis(List<String> painKeys,
                                        Map<String, Map<String, Object>> overallSummary,
                                        Map<String, String> standardRanges) {
        if (painKeys == null || painKeys.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < painKeys.size(); i++) {
            String painKey = painKeys.get(i);
            String painDesc = RidingPainType.getDescriptionByCode(painKey);
            Set<String> relatedMetrics = PAIN_METRIC_MAP.get(painKey);
            String hint = PAIN_ADJUSTMENT_HINTS.getOrDefault(painKey, "请关注相关指标");

            if (relatedMetrics == null || relatedMetrics.isEmpty()) {
                continue;
            }

            // 收集关联指标的异常值信息
            List<String> detailParts = new ArrayList<>();
            for (String metricKey : relatedMetrics) {
                // 从 overallSummary 中查找该指标的值
                Double metricVal = findMetricValue(overallSummary, metricKey);
                if (metricVal == null) continue;

                // 获取该指标的标准范围
                String range = resolveIdealRange(metricKey, null, standardRanges);
                String status = resolveStatusFromRange(metricVal, range);

                // 只展示异常(WARNING)和需要调整(ADJUST)的指标
                if ("WARNING".equals(status) || "ADJUST".equals(status)) {
                    String label = getMetricLabel(metricKey);
                    detailParts.add(String.format("%s偏%s(%.1f°，理想%s)", label,
                            "WARNING".equals(status) ? "离" : "差", metricVal, range));
                }
            }

            if (sb.length() > 0) {
                sb.append("；");
            }
            if (detailParts.isEmpty()) {
                sb.append(String.format("您反馈的%s，当前关联指标均在理想范围内，请继续保持当前设定", painDesc));
            } else {
                sb.append(String.format("您反馈的%s，可能与%s有关，%s",
                        painDesc, String.join("和", detailParts), hint));
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 从 overallSummary 中查找指定指标的值
     */
    private Double findMetricValue(Map<String, Map<String, Object>> overallSummary, String metricKey) {
        if (overallSummary == null) return null;
        for (Map<String, Object> viewMetrics : overallSummary.values()) {
            if (viewMetrics != null && viewMetrics.containsKey(metricKey)) {
                Object val = viewMetrics.get(metricKey);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                }
            }
        }
        return null;
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
        labels.put("rightKneeWobble", "右膝横摆");
        labels.put("leftKneeWobble", "左膝横摆");
        labels.put("rightElbowFlare", "右肘外张");
        labels.put("leftElbowFlare", "左肘外张");
        
        // 稳定性与汇总
        labels.put("ankleY", "脚踝轨迹");
        labels.put("avgTorso", "平均躯干角度");
        labels.put("minHip", "上死点最小髋角");
        labels.put("maxHip", "下死点最大髋角");
        labels.put("avgKneeLateral", "膝盖横向偏移");
        labels.put("maxKnee", "下死点膝伸展角");
        labels.put("maxShoulder", "最大肩角 (前伸量)");

        // 背面
        labels.put("maxPelvisSway", "骨盆横摆位移");
        labels.put("maxSpineTilt", "脊柱中线偏移");
        labels.put("spineRockingVar", "脊柱侧偏波动");
        labels.put("hipDrop_L", "左侧骨盆下坠");
        labels.put("hipDrop_R", "右侧骨盆下坠");

        // 【修复 P14】稳定性指标（StdDev / CV / Variance）
        labels.put("kneeAnglePhaseStdDev", "膝角相位一致性");
        labels.put("hipAnglePhaseStdDev", "髋角相位一致性");
        labels.put("ankleAnglePhaseStdDev", "踝角相位一致性");
        labels.put("torsoAngleStdDev", "躯干稳定性");
        labels.put("leftKneeAngleStdDev", "左膝角稳定性");
        labels.put("rightKneeAngleStdDev", "右膝角稳定性");
        labels.put("shoulderTiltStdDev", "肩膀稳定性");
        labels.put("hipTiltStdDev", "骨盆侧倾稳定性");
        labels.put("leftKneeWobbleStdDev", "左膝横摆稳定性");
        labels.put("rightKneeWobbleStdDev", "右膝横摆稳定性");
        labels.put("leftElbowFlareStdDev", "左肘外张稳定性");
        labels.put("rightElbowFlareStdDev", "右肘外张稳定性");
        labels.put("pelvisSwayStdDev", "骨盆摇摆稳定性");
        labels.put("spineTiltStdDev", "脊柱偏斜稳定性");
        labels.put("leftHipDropStdDev", "左髋下坠稳定性");
        labels.put("rightHipDropStdDev", "右髋下坠稳定性");

        // 兜底：对未知的 StdDev/CV/Variance 后缀，尝试去掉后缀从已有标签推断
        if (!labels.containsKey(key)) {
            if (key.endsWith("PhaseStdDev")) {
                String base = labels.get(key.replace("PhaseStdDev", ""));
                if (base != null) return base + "相位一致性";
            } else if (key.endsWith("StdDev")) {
                String base = labels.get(key.replace("StdDev", ""));
                if (base != null) return base + "稳定性";
            } else if (key.endsWith("CV")) {
                String base = labels.get(key.replace("CV", ""));
                if (base != null) return base + "变异系数";
            } else if (key.endsWith("Variance")) {
                String base = labels.get(key.replace("Variance", ""));
                if (base != null) return base + "波动方差";
            }
        }

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
        if (score >= 90) sb.append("您的骑行姿态与车型高度适配，各项指标表现优秀，继续保持！");
        else if (score >= 80) sb.append("整体姿态表现良好，少数细节仍有微调空间，建议关注下方标注「建议调整」的指标。");
        else if (score >= 70) sb.append("系统发现若干项关键姿态偏差，通过针对性微调可以显著提升您的骑行舒适度和效率。");
        else sb.append("您的骑行姿态与车辆设定存在明显偏差，建议对车辆规格进行较大幅度调整，或咨询专业 Fitter。");

        // 2. 痛点关联 (解析 Session 中的 painLevels)
        String painJson = result.getSession().getPainLevels();
        if (painJson != null && !painJson.isEmpty()) {
            try {
                List<String> painKeys = JSON.parseArray(painJson, String.class);
                if (painKeys != null && !painKeys.isEmpty()) {
                    List<String> labels = painKeys.stream()
                            .map(RidingPainType::getDescriptionByCode)
                            .collect(Collectors.toList());

                    sb.append(String.format(" 针对您反馈的 [%s] 等不适部位，系统已结合指标偏差为您提供专项优化建议：", String.join("、", labels)));
                }
            } catch (Exception ignored) {
            }
        }

        // 3. 建议数量统计
        if (advices != null && !advices.isEmpty()) {
            long adjustCount = advices.size();
            sb.append(String.format(" 共包含 %d 项关键调整指导，建议按顺序逐项检查。", adjustCount));
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
     * 计算时序数据的稳定性指标。
     * 周期性关节角（膝/髋/踝）按踩踏相位重采样后计算跨周期一致性，
     * 姿态和横向控制指标直接计算原始波动，避免把正常踩踏周期误判为不稳定。
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
            if (CYCLIC_SIDE_METRICS.contains(metric)) {
                Double phaseStdDev = calculatePhaseConsistencyStdDev(values);
                if (phaseStdDev != null) {
                    stats.put(metric + "PhaseStdDev", round2(phaseStdDev));
                }
                stats.put(metric + "Mean", round2(mean(values)));
                return;
            }

            if (!POSTURE_STABILITY_METRICS.contains(metric)) return;
            if (values.size() < 5) return;

            double mean = mean(values);
            double variance = variance(values, mean);
            double stdDev = Math.sqrt(variance);
            double cv = (Math.abs(mean) > 0.001) ? (stdDev / Math.abs(mean)) * 100 : 0;

            stats.put(metric + "Mean", round2(mean));
            stats.put(metric + "StdDev", round2(stdDev));
            stats.put(metric + "CV", round2(cv));
            stats.put(metric + "Variance", round2(variance));
        });

        return stats;
    }

    private Double calculatePhaseConsistencyStdDev(List<Double> values) {
        if (values == null || values.size() < 30) return null;
        List<Integer> peaks = findPeaks(values, 4);
        if (peaks.size() < 3) return null;

        int phaseBins = 24;
        List<List<Double>> bins = new ArrayList<>(phaseBins);
        for (int i = 0; i < phaseBins; i++) bins.add(new ArrayList<>());

        for (int p = 0; p < peaks.size() - 1; p++) {
            int start = peaks.get(p);
            int end = peaks.get(p + 1);
            if (end <= start + 4) continue;
            for (int b = 0; b < phaseBins; b++) {
                // 避免将下一个周期的起始点采样到当前周期，b/(phaseBins-1)保证最后一个点不超过end-1
                int idx = start + (int) Math.round((end - start) * (b / (double)(phaseBins - 1)));
                idx = Math.min(idx, end - 1); // 严格限制在当前周期内（start到end-1）
                bins.get(b).add(values.get(idx));
            }
        }

        double sumStd = 0;
        int validBins = 0;
        for (List<Double> bin : bins) {
            if (bin.size() < 2) continue;
            double m = mean(bin);
            sumStd += Math.sqrt(variance(bin, m));
            validBins++;
        }
        return validBins > 0 ? sumStd / validBins : null;
    }

    private List<Integer> findPeaks(List<Double> data, int windowSize) {
        List<Integer> peaks = new ArrayList<>();
        if (data == null || data.size() < windowSize * 2 + 1) return peaks;

        for (int i = windowSize; i < data.size() - windowSize; i++) {
            double val = data.get(i);
            boolean isPeak = true;
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j != i && data.get(j) >= val) {
                    isPeak = false;
                    break;
                }
            }
            if (isPeak && (peaks.isEmpty() || i - peaks.get(peaks.size() - 1) > windowSize)) {
                peaks.add(i);
            }
        }
        return peaks;
    }

    private double mean(List<Double> values) {
        double sum = 0;
        for (Double v : values) sum += v;
        return sum / values.size();
    }

    private double variance(List<Double> values, double mean) {
        double sumSqDiff = 0;
        for (Double v : values) {
            sumSqDiff += Math.pow(v - mean, 2);
        }
        // 使用样本方差 n-1（生物力学标准做法），避免小样本低估
        return values.size() > 1 ? sumSqDiff / (values.size() - 1) : sumSqDiff;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
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
