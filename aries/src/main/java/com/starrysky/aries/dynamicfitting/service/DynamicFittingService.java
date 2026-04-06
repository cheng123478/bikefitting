package com.starrysky.aries.dynamicfitting.service;

import com.alibaba.fastjson2.JSON;
import com.starrysky.aries.dynamicfitting.algorithm.DynamicFittingAlgorithm;
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
import com.starrysky.aries.util.upload.FileUploadResult;
import com.starrysky.aries.util.upload.FileUploadUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

                        String imageUrl = uploadBase64Image(base64, userId, viewType, tag);

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
                overallSummary.put(viewType, summary);
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
     * 根据用户和车辆查询采集列表 (分页)
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
     */
    public Map<String, Object> getSessionDetail(Long sessionId) {
        DynamicFittingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        Map<String, Object> detail = new HashMap<>();
        detail.put("sessionVO", convertToVO(session));
        return detail;
    }

    private DynamicFittingSessionVO convertToVO(DynamicFittingSession session) {
        DynamicFittingSessionVO vo = new DynamicFittingSessionVO();

        // 拷贝基本属性 (id, status, sessionTime, deviceInfo 等)
        BeanUtils.copyProperties(session, vo);

        // 处理关联的单车信息 (防止懒加载异常并提取展示所需字段)
        if (session.getUserBike() != null) {
            vo.setBikeId(session.getUserBike().getId());
            vo.setRemark(session.getUserBike().getRemark());
            // 重要：直接把名字带出去，这样前端历史记录列表就不用再查单车接口了
            vo.setBikeName(
                    session.getUserBike().getBikeModel() != null ? session.getUserBike().getBikeModel().getModel()
                            : "未知车型");
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
        if (session.getStatus() != null && session.getStatus() == FittingStatus.COMPLETED.getCode()) {
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
                new com.alibaba.fastjson2.TypeReference<Map<String, Map<String, Object>>>() {
                });

        if (overallSummary != null) {
            reportVO.setSideData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO(overallSummary.get("side"), advices)));
            reportVO.setFrontData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO(overallSummary.get("front"), advices)));
            reportVO.setBackData(
                    new DynamicFittingReportVO.ViewData(mapToMetricVO(overallSummary.get("back"), advices)));
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

    private List<DynamicFittingMetricVO> mapToMetricVO(Map<String, Object> metrics,
            List<DynamicFittingAdviceVO> advices) {
        if (metrics == null)
            return Collections.emptyList();

        List<DynamicFittingMetricVO> vos = new ArrayList<>();
        metrics.forEach((key, val) -> {
            // 过滤并处理统计指标 (Median/Val/Mean)
            if (key.endsWith("Median") || key.endsWith("Val") || key.endsWith("Mean")) {
                String baseKey = key.replaceAll("(Median|Val|Mean)$", "");

                // 查找对应的诊断建议
                DynamicFittingAdviceVO advice = advices.stream()
                        .filter(a -> a.getMetric().equalsIgnoreCase(baseKey) || a.getMetric().equalsIgnoreCase(key))
                        .findFirst().orElse(null);

                String label = getMetricLabel(baseKey);
                if (label.equals(baseKey)) { // 如果没翻译，尝试翻译 baseKey 的 baseKey
                    // 处理类似 frontMaxLKnee -> kneeAngle 的嵌套映射 (简单处理)
                }

                vos.add(DynamicFittingMetricVO.builder()
                        .label(label)
                        .currentValue(String.format("%.1f", convertToDouble(val)))
                        .status(advice != null ? (advice.getType() != null ? advice.getType().toUpperCase() : "ADJUST") : "OK")
                        .idealRange(advice != null ? (advice.getRange() != null ? advice.getRange() : "--") : "--")
                        .advice(advice != null ? (advice.getAdviceContent() != null ? advice.getAdviceContent() : advice.getCode()) : "处于理想范围")
                        .build());
            } else if (key.equals("avgTorso") || key.equals("minHip") || key.equals("maxSpineTilt") || key.equals("maxPelvisSway")) {
                // 处理一些不带后缀但也是核心指标的字段
                DynamicFittingAdviceVO advice = advices.stream()
                        .filter(a -> a.getMetric().equalsIgnoreCase(key))
                        .findFirst().orElse(null);

                vos.add(DynamicFittingMetricVO.builder()
                        .label(getMetricLabel(key))
                        .currentValue(String.format("%.1f", convertToDouble(val)))
                        .status(advice != null ? "ADJUST" : "OK")
                        .idealRange(advice != null ? advice.getRange() : "--")
                        .advice(advice != null ? (advice.getAdviceContent() != null ? advice.getAdviceContent() : advice.getCode()) : "处于理想范围")
                        .build());
            }
        });
        return vos;
    }

    private String getMetricLabel(String key) {
        Map<String, String> labels = new HashMap<>();
        // 侧面
        labels.put("kneeAngle", "膝角 (伸展度)");
        labels.put("hipAngle", "髋角 (重心枢轴)");
        labels.put("torsoAngle", "背部角度");
        labels.put("ankleAngle", "踝角");
        labels.put("elbowAngle", "肘部角度");
        // 正面
        labels.put("avgLeftKnee", "左膝中值角度");
        labels.put("avgRightKnee", "右膝中值角度");
        labels.put("maxWobble", "膝盖最大横摆");
        labels.put("maxTilt", "身体最大侧倾");
        // 背面
        labels.put("maxPelvisSway", "骨盆横移量");
        labels.put("maxSpineTilt", "脊柱侧中心偏移");
        labels.put("maxLeftHipDrop", "左侧骨盆下坠");
        labels.put("maxRightHipDrop", "右侧骨盆下坠");

        // 核心原始指标映射
        labels.put("avgTorso", "平均躯干角度");
        labels.put("minHip", "曲柄顶点最小髋角");
        labels.put("maxHip", "曲柄底端最大髋角");
        labels.put("avgKneeLateral", "膝盖横向偏移均值");

        return labels.getOrDefault(key, key);
    }

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
            } catch (Exception ignored) {}
        }

        // 3. 建议数量统计
        if (advices != null && !advices.isEmpty()) {
            long adjustCount = advices.stream().filter(a -> "adjust".equals(a.getType())).count();
            if (adjustCount > 0) {
                sb.append(String.format("共包含 %d 项关键调整指导。", adjustCount));
            }
        }

        return sb.toString();
    }

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
     * 获取诊断简介 (用于列表展示)
     */
    public DynamicFittingResultVO getFittingResult(Long sessionId) {
        DynamicFittingResult result = resultRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new BizException("报告尚未生成或不存在"));
        DynamicFittingResultVO resultVO = new DynamicFittingResultVO();
        BeanUtils.copyProperties(result, resultVO);
        resultVO.setMatchLevel(getMatchLevel(result.getMatchScore()));

        // 摘要补充
        if (resultVO.getSummary() == null) {
            List<DynamicFittingAdviceVO> advices = JSON.parseArray(result.getAdviceJson(),
                    DynamicFittingAdviceVO.class);
            if (advices != null && !advices.isEmpty()) {
                resultVO.setSummary(advices.get(0).getCode());
            }
        }
        return resultVO;
    }

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
                String key = url.substring(url.indexOf("fitting/"));
                cosUtil.delete(key);
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

    private String uploadBase64Image(String base64, Long userId, String viewType, String tag) {
        try {
            // 去掉 data:image/png;base64, 前缀
            String data = base64;
            if (base64.contains(",")) {
                data = base64.substring(base64.indexOf(",") + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(data);

            String middlePath = "/" + userId + "/fitting/";
            FileUploadResult fileUploadResult = fileUploadUtil.uploadPic(middlePath, bytes);
            return fileUploadResult.getUrl();

            // String key = String.format("fitting/%d/%s_%s_%s.png",
            // sessionId, viewType, tag, UUID.randomUUID().toString().substring(0, 8));
            //
            // return cosUtil.upload(new ByteArrayInputStream(bytes), bytes.length, key);
        } catch (Exception e) {
            log.error("上传 Base64 图片失败: {} - {}", viewType, tag, e);
            return "";
        }
    }
}
