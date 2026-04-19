package com.starrysky.aries.staticfitting.service;

import com.starrysky.aries.entity.*;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.staticfitting.repository.StaticFittingResultRepository;
import com.starrysky.aries.staticfitting.schema.*;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import com.starrysky.aries.userbike.repository.UserBikeSetupRepository;
import com.starrysky.aries.userbody.repository.UserBodyRepository;
import com.starrysky.aries.util.BikeType;
import com.starrysky.aries.util.RidingPainType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 本服务负责协调身体数据、单车数据及生物力学算法，生成分层级的调教报告。
 *
 * @author adi
 * @version 1.0
 * @since 2025-12-23
 */
@Service
@Slf4j
public class StaticFittingService {
    @Resource
    private FittorDiagnosticsManager diagnosticsManager;
    @Resource
    private UserBodyRepository userBodyRepository;
    @Resource
    private UserBikeRepository userBikeRepository;
    @Resource
    private UserBikeSetupRepository userBikeSetupRepository;
    @Resource
    private StaticFittingResultRepository staticFittingResultRepository;

    /**
     * 获取用户的历史调教记录
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 历史调教记录
     */
    public Page<StaticFittingResultVO> getHistoryByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Object[]> dbPage = staticFittingResultRepository.findHistoryByUserId(userId, pageable);

        return dbPage.map(row -> {
            StaticFittingResult entity = (StaticFittingResult) row[0];
            String bikeName = (String) row[1];
            return convertToVO(entity, bikeName);
        });
    }

    /**
     * 计算静态调教
     *
     * @param request 静态调教请求
     * @return 静态调教结果
     */
    public StaticFittingResultVO calculateStaticFitting(StaticFittingRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long bikeId = request.getUserBike() != null ? request.getUserBike().getId() : null;
        if (bikeId == null) {
            throw new BizException("进行诊断前请选择一辆实车或设定档");
        }

        UserBody body = userBodyRepository.findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow(() -> new BizException("未找到身体测量数据，请先录入[身体图谱]"));

        validateCoreData(body);

        UserBike bike = userBikeRepository.findById(bikeId).orElseThrow(() -> new BizException("找不到指定的单车信息"));
        UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId())
                .orElse(new UserBikeSetup());

        StaticFittingResult result = new StaticFittingResult();
        initializeResult(result, body, bike);

        try {
            // A. 数据质量校验
            int validationCode = StaticFittingCalculator.validateHumanProportions(body.getSpanHeight(), body.getHeight(), body.getLongArmLength());
            if (validationCode > 0) {
                String templateKey = switch (validationCode) {
                    case 1 -> "span_ratio_high";
                    case 2 -> "span_ratio_low";
                    case 3 -> "ape_index_warning";
                    default -> "";
                };
                if (!templateKey.isEmpty()) {
                    diagnosticsManager.addValidationDetail(result, "数据质量", "异常提示", templateKey);
                }
            }

            // B. 多层级生物力学计算与诊断
            processLevel1Sizing(body, setup, bike.getBikeModel().getBikeType(), result);
            processLevel2Ergonomics(user, body, bike.getBikeModel().getBikeType(), result);
            diagnosticsManager.diagnoseComponents(result, body, setup);
            processLevel3Biomechanical(body, bike, setup, result);

            // C. 现状对比与痛点修正
            processBikeAdjustment(bike, setup, result);
            diagnosticsManager.addAdjustmentAdvice(result, bike, setup);

            List<RidingPainType> painLevels = request.getPainLevels();
            if (painLevels != null && !painLevels.isEmpty()) {
                result.setInputPainTypes(new ArrayList<>(painLevels));
                diagnosticsManager.diagnosePain(result, painLevels);
            }

            diagnosticsManager.diagnoseSafety(result, body);
            assembleFinalPostureSuggestion(result);

        } catch (Exception e) {
            log.error("Fitting Engine Error: ", e);
            result.setPostureSuggestion("计算过程中由于部分数据异常导致中断: " + e.getMessage());
        }

        StaticFittingResult savedResult = staticFittingResultRepository.save(result);
        return convertToVO(savedResult, bike.getBikeModel() != null ? bike.getBikeModel().getModel() : "未知车型");
    }

    private void validateCoreData(UserBody body) {
        if (body.getHeight() == null || body.getHeight() < 1200 || body.getHeight() > 2200 ||
                body.getSpanHeight() == null || body.getSpanHeight() < 600 || body.getSpanHeight() > 1000) {
            throw new BizException("基础数据(身高/跨高)疑似异常或缺失，请确认测量单位(mm)及数值准确性。");
        }
    }

    /**
     * 将实体对象转换为VO对象
     *
     * @param entity   静态调教结果实体对象
     * @param bikeName 单车名称
     * @return 静态调教结果VO对象
     */
    private StaticFittingResultVO convertToVO(StaticFittingResult entity, String bikeName) {
        StaticFittingResultVO vo = new StaticFittingResultVO();
        vo.setId(entity.getId());
        vo.setFittingTime(entity.getFittingTime());
        vo.setUserBodyId(entity.getUserBodyId());
        vo.setUserBikeId(entity.getUserBikeId());
        vo.setThoracicScore(entity.getThoracicScore());
        vo.setRecSeatHeight(entity.getRecSeatHeight());
        vo.setRecSetback(entity.getRecSetback());
        vo.setRecReachPlus(entity.getRecReachPlus());
        vo.setRecFrameStack(entity.getRecFrameStack());
        vo.setTargetBarHeight(entity.getTargetBarHeight());
        vo.setRecHandlebarDrop(entity.getRecHandlebarDrop());
        vo.setRecSpacerHeight(entity.getRecSpacerHeight());
        vo.setRecCrankLength(entity.getRecCrankLength());
        vo.setRecHandlebarWidth(entity.getRecHandlebarWidth());
        vo.setRecSaddleWidth(entity.getRecSaddleWidth());
        vo.setRecStemLength(entity.getRecStemLength());
        vo.setPostureSuggestion(entity.getPostureSuggestion());
        vo.setRiderTags(entity.getRiderTags());
        vo.setDetails(entity.getDetails());

        vo.setBikeName(bikeName != null ? bikeName : "纯人体测量基准");

        if (entity.getInputPainTypes() != null) {
            vo.setPainLabels(entity.getInputPainTypes().stream()
                    .map(RidingPainType::getDescription)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * 初始化调教结果对象
     *
     * @param body     用户身体数据
     * @param setup    用户单车设定数据
     * @param bikeType 单车类型
     * @param res      调教结果对象
     */
    private void processLevel1Sizing(UserBody body, UserBikeSetup setup, int bikeType, StaticFittingResult res) {
        int baseHeight = StaticFittingCalculator.calcBaseSeatHeight(body.getSpanHeight(), bikeType);

        if (setup != null && setup.getPedalType() != null) {
            if (setup.getPedalType() == 1) baseHeight -= 10;
            else if (setup.getPedalType() == 3) baseHeight -= 3;
        }
        res.setRecSeatHeight(baseHeight);
        res.setRecFrameStack(StaticFittingCalculator.calcBaseStack(body.getSpanHeight(), bikeType));
        res.setRecCrankLength(StaticFittingCalculator.calcCrankLength(body.getSpanHeight(), bikeType));
        res.getRiderTags().add("核心基准已生成");
    }

    /**
     * 处理二级调教
     *
     * @param user     用户信息
     * @param body     用户身体数据
     * @param bikeType 单车类型
     * @param res      调教结果对象
     */
    private void processLevel2Ergonomics(User user, UserBody body, int bikeType, StaticFittingResult res) {
        if (body.getIschialDistance() != null && body.getIschialDistance() > 0) {
            res.setRecSaddleWidth(StaticFittingCalculator.calcSaddleWidth(body.getIschialDistance(), bikeType));
        } else {
            int defaultIschial = (user.getGender() != null && user.getGender() == 2) ? 155 : 143;
            res.setRecSaddleWidth(StaticFittingCalculator.calcSaddleWidth(defaultIschial, bikeType));
            addIncompleteNote(res, "器材匹配", "missing_ischial");
        }

        if (body.getShoulderWidth() != null && body.getShoulderWidth() > 0) {
            int handleWidth = (bikeType == BikeType.ROAD_BIKE.code) ? (body.getShoulderWidth() / 10 * 10) : (body.getShoulderWidth() + 300);
            res.setRecHandlebarWidth(handleWidth);
        } else {
            int defaultShoulder = (user.getGender() != null && user.getGender() == 2) ? 380 : 400;
            int handleWidth = (bikeType == BikeType.ROAD_BIKE.code) ? (defaultShoulder / 10 * 10) : (defaultShoulder + 300);
            res.setRecHandlebarWidth(handleWidth);
            addIncompleteNote(res, "器材匹配", "missing_shoulder");
        }
    }

    /**
     * 处理三级调教
     *
     * @param body  用户身体数据
     * @param bike  用户单车数据
     * @param setup 用户单车设定数据
     * @param res   调教结果对象
     */
    private void processLevel3Biomechanical(UserBody body, UserBike bike, UserBikeSetup setup, StaticFittingResult res) {

        int tScore = 0;
        if (body.getThoracicRotationAngle() != null && body.getArmRaiseAngle() != null) {
            int pain = (body.getThoracicPainLevel() != null) ? body.getThoracicPainLevel() : 0;
            tScore = (int) (body.getThoracicRotationAngle() * 0.6 + body.getArmRaiseAngle() * 0.4 - pain * 10);
            res.setThoracicScore(Math.max(0, tScore));
        }

        if (body.getThighLength() != null && body.getThighLength() > 0 && body.getSpanHeight() != null) {
            double femurRatio = (double) body.getThighLength() / body.getSpanHeight();
            int tibiaCorrection = StaticFittingCalculator.calcTibiaCorrection(body.getCalfLength() != null ? body.getCalfLength() : 0, body.getSpanHeight());

            res.setRecSeatHeight(res.getRecSeatHeight() + tibiaCorrection);
            res.setRecSetback(StaticFittingCalculator.calcSetback(body.getThighLength(), femurRatio));
        }

        if (body.getBreastHeight() != null && body.getLongArmLength() != null && body.getSpanHeight() != null) {
            int trunk = body.getBreastHeight() - body.getSpanHeight();
            int arms = body.getLongArmLength() + (body.getShortArmLength() != null ? body.getShortArmLength() : body.getLongArmLength());

            res.setRecReachPlus((int) Math.round(StaticFittingCalculator.calcReachPlus(trunk, arms, tScore, bike.getBikeModel().getBikeType())));

            int h = (body.getHeight() != null) ? body.getHeight() : 1700;
            double ang = (body.getArmRaiseAngle() != null) ? body.getArmRaiseAngle() : 180.0;
            double drop = StaticFittingCalculator.calcHandlebarDrop(h, ang, tScore, bike.getBikeModel().getBikeType());
            res.setRecHandlebarDrop((int) Math.round(drop));
            res.setTargetBarHeight(res.getRecSeatHeight() - (int) Math.round(drop));
        } else {
            addIncompleteNote(res, "上肢座舱", "missing_upper_body");
        }

        double femurRatio = (body.getThighLength() != null && body.getSpanHeight() != null) ? (double) body.getThighLength() / body.getSpanHeight() : 0.46;
        double apeIndex = (body.getLongArmLength() != null) ? (double) (body.getLongArmLength() * 2) / body.getHeight() : 1.0;

        diagnosticsManager.diagnosePortrait(res, body, apeIndex, femurRatio);
        diagnosticsManager.diagnoseLowerBody(res, femurRatio, setup);
        diagnosticsManager.diagnoseUpperBody(res, body, setup);
    }

    /**
     * 处理单车调教
     *
     * @param bike  用户单车数据
     * @param setup 用户单车设定数据
     * @param res   调教结果对象
     */
    private void processBikeAdjustment(UserBike bike, UserBikeSetup setup, StaticFittingResult res) {
        if (res.getRecReachPlus() == null || bike.getBikeModel() == null || bike.getBikeModel().getReach() == null)
            return;
        double stemRaw = res.getRecReachPlus() - bike.getBikeModel().getReach();
        if (setup == null || setup.getStemAngle() == null || setup.getStemLength() == null) {
            addIncompleteNote(res, "调教方案", "missing_stem_angle");
            return;
        }
        double stemOffset = StaticFittingCalculator.calculateReachOffset(5, setup.getStemAngle()) / 5.0;
        res.setRecStemLength((int) (Math.round((stemRaw / (stemOffset / 5.0)) / 5.0) * 5));

        // 计算建议垫圈高度: Spacer = targetBarHeight - frameStack - stemLength * sin(stemAngle)
        if (res.getTargetBarHeight() != null && bike.getBikeModel().getStack() != null
                && res.getRecStemLength() != null && setup.getStemAngle() != null) {
            double stemVertical = res.getRecStemLength() * Math.sin(Math.toRadians(Math.abs(setup.getStemAngle())));
            int spacer = (int) Math.round(res.getTargetBarHeight() - bike.getBikeModel().getStack() - stemVertical);
            res.setRecSpacerHeight(Math.max(spacer, 0));
        }
    }

    /**
     * 诊断单车调教
     *
     * @param userId     用户ID
     * @param userBikeId 用户单车ID
     * @return 单车调教报告
     */
    public StaticFittingReportVO diagnoseBikeSetup(Long userId, Long userBikeId) {
        UserBody body = userBodyRepository.findTopByUserIdOrderByIdDesc(userId).orElseThrow(() -> new BizException("身体数据不存在"));
        UserBike bike = userBikeRepository.findById(userBikeId).orElseThrow(() -> new BizException("车辆数据不存在"));
        UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId()).orElse(new UserBikeSetup());

        StaticFittingResult ideal = staticFittingResultRepository.findTopByUserBodyIdAndUserBikeIdOrderByFittingTimeDesc(body.getId(), bike.getId())
                .orElseThrow(() -> new BizException("请先生成当前车辆的 Fitting 方案"));

        StaticFittingReportVO report = new StaticFittingReportVO();
        report.setFittingResultId("FIT-" + ideal.getId());
        report.setCreateTime(ideal.getFittingTime().toString());

        // 车架几何匹配检查 (Stack & Reach)
        if (bike.getBikeModel() != null) {
            // Stack 匹配检查
            if (ideal.getRecFrameStack() != null && bike.getBikeModel().getStack() != null) {
                int stackDelta = Math.abs(ideal.getRecFrameStack() - bike.getBikeModel().getStack());
                String stackStatus = stackDelta <= 10 ? "PERFECT" : (stackDelta <= 25 ? "ADJUST" : "WARNING");
                String stackReason = stackDelta <= 10 ? "车架 Stack 与推荐值完美匹配" :
                    (stackDelta <= 20 ? "车架 Stack 略有偏差，可通过垫圈微调" : "车架 Stack 偏差较大，后期调教受限");
                report.setFrameStackCheck(buildMetric("Stack", ideal.getRecFrameStack(), bike.getBikeModel().getStack(), 10, stackReason, stackStatus));
            }
            // Reach 匹配检查
            if (ideal.getRecReachPlus() != null && bike.getBikeModel().getReach() != null) {
                int reachDelta = Math.abs(ideal.getRecReachPlus() - bike.getBikeModel().getReach());
                String reachStatus = reachDelta <= 5 ? "PERFECT" : (reachDelta <= 15 ? "ADJUST" : "WARNING");
                String reachReason = reachDelta <= 5 ? "车架 Reach 与推荐值完美匹配" :
                    (reachDelta <= 20 ? "车架 Reach 略有偏差，可通过把立微调" : "车架 Reach 偏差较大，建议更换车架尺码");
                report.setFrameReachCheck(buildMetric("Reach", ideal.getRecReachPlus(), bike.getBikeModel().getReach(), 10, reachReason, reachStatus));
            }
        }

        List<StaticFittingMetricVO> mechanicalAdjustments = new ArrayList<>();
        List<String> expertDiagnoses = new ArrayList<>();
        List<HardwareUpgradeVO> hardwareUpgrades = new ArrayList<>();
        double maxDelta = 0;

        if (ideal.getRecSeatHeight() != null && setup.getSaddleHeight() != null) {
            StaticFittingMetricVO m = buildMetric("Saddle Height (座垫高度)", ideal.getRecSeatHeight(), setup.getSaddleHeight(), 5, "决定踩踏效率与膝盖健康");
            mechanicalAdjustments.add(m);
            maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().intValue()));
            if (Math.abs(m.getDelta().intValue()) > 20)
                expertDiagnoses.add(m.getDelta().intValue() > 0 ? "🔴 座垫显著偏低，易造成膝前受压。" : "🔴 座垫显著偏高，易造成大腿后侧拉伤。");
        }

        if (ideal.getRecReachPlus() != null && bike.getBikeModel() != null && bike.getBikeModel().getReach() != null && setup.getStemLength() != null) {
            double currentReachPlus = calculateCurrentReachPlus(bike.getBikeModel(), setup);
            StaticFittingMetricVO m = buildMetric("Reach+ (有效前伸量)", ideal.getRecReachPlus(), currentReachPlus, 10, "影响重心分布与操控舒适度");
            mechanicalAdjustments.add(m);
            maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().intValue()));
            if (Math.abs(m.getDelta().intValue()) > 25)
                expertDiagnoses.add("🔴 骑行姿态过" + (m.getDelta().intValue() > 0 ? "短" : "长") + "，建议调整把立长度。");
        }

        // 坐垫后移
        if (ideal.getRecSetback() != null && setup.getSaddleSetback() != null) {
            StaticFittingMetricVO m = buildMetric("Setback (座垫后移)", ideal.getRecSetback(), setup.getSaddleSetback(), 5, "影响发力角度与膝盖健康");
            mechanicalAdjustments.add(m);
        }

        // 曲柄长度
        if (ideal.getRecCrankLength() != null && setup.getCrankLength() != null) {
            StaticFittingMetricVO m = buildMetric("Crank Length (曲柄长度)", ideal.getRecCrankLength(), setup.getCrankLength(), 2, "影响踩踏效率与膝盖负荷");
            mechanicalAdjustments.add(m);
        }

        // 坐垫推荐宽
        if (ideal.getRecSaddleWidth() != null && setup.getSaddleWidth() != null) {
            StaticFittingMetricVO m = buildMetric("Saddle Width (坐垫推荐宽)", ideal.getRecSaddleWidth(), setup.getSaddleWidth(), 5, "影响坐骨支撑与舒适度");
            mechanicalAdjustments.add(m);
        }

        // 车把落差
        if (ideal.getRecHandlebarDrop() != null && setup.getHandlebarDrop() != null) {
            StaticFittingMetricVO m = buildMetric("Handlebar Drop (车把落差)", ideal.getRecHandlebarDrop(), setup.getHandlebarDrop(), 10, "影响骑行姿态与风阻");
            mechanicalAdjustments.add(m);
        }

        // 垫圈高度
        if (ideal.getRecSpacerHeight() != null && setup.getSpacersHeight() != null) {
            StaticFittingMetricVO m = buildMetric("Spacer Height (垫圈高度)", ideal.getRecSpacerHeight(), setup.getSpacersHeight(), 5, "影响车把高度与舒适度");
            mechanicalAdjustments.add(m);
        }

        // 把立长度
        if (ideal.getRecStemLength() != null && setup.getStemLength() != null) {
            StaticFittingMetricVO m = buildMetric("Stem Length (把立长度)", ideal.getRecStemLength(), setup.getStemLength(), 5, "影响前伸量与操控");
            mechanicalAdjustments.add(m);
        }

        if (ideal.getRecCrankLength() != null && setup.getCrankLength() != null && !ideal.getRecCrankLength().equals(setup.getCrankLength())) {
            HardwareUpgradeVO h = new HardwareUpgradeVO();
            h.setPart("曲柄长度");
            h.setCurrentValue(setup.getCrankLength() + "mm");
            h.setTargetSpec(ideal.getRecCrankLength() + "mm");
            h.setReason("您的跨高对应的最佳曲柄杠杆比为" + ideal.getRecCrankLength() + "mm。");
            hardwareUpgrades.add(h);
        }

        report.setMechanicalAdjustments(mechanicalAdjustments);
        report.setExpertDiagnoses(expertDiagnoses);
        report.setHardwareUpgrades(hardwareUpgrades);

        if (maxDelta > 40) {
            report.setOverallStatus("DANGER");
            report.setMatchLevel("偏差极大");
            report.setSummary("🚨 警告：当前设定与基准偏离超过 40mm，切勿盲目调整，建议核实测量数据或车架尺码。");
        } else if (maxDelta > 10) {
            report.setOverallStatus("WARNING");
            report.setMatchLevel("需优化");
            report.setSummary("检测到关键参数存在明显偏差。");
        } else {
            report.setOverallStatus("PASS");
            report.setMatchLevel("完美契合");
            report.setSummary("当前设定非常理想，尽情享受骑行！");
        }
        return report;
    }

    /**
     * 添加诊断单车调教
     *
     * @param res   单车调教结果
     * @param part  部位
     * @param templateKey 调整逻辑
     */
    private void addIncompleteNote(StaticFittingResult res, String part, String templateKey) {
        diagnosticsManager.addValidationDetail(res, part, "数据缺失", templateKey);
    }

    /**
     * 计算当前前伸量
     *
     * @param model 单车模型
     * @param setup 单车调教
     * @return 当前前伸量
     */
    private double calculateCurrentReachPlus(BikeModel model, UserBikeSetup setup) {
        return model.getReach() + StaticFittingCalculator.calculateReachOffset(setup.getStemLength(), setup.getStemAngle());
    }

    /**
     * 组装最终调教建议
     *
     * @param res 单车调教结果
     */
    private void assembleFinalPostureSuggestion(StaticFittingResult res) {
        String fullReport = res.getDetails().stream()
                .map(d -> "【" + d.getPart() + "】" + d.getAdjustmentLogic())
                .collect(Collectors.joining("\n\n"));
        res.setPostureSuggestion(fullReport);
    }

    /**
     * 初始化单车调教结果
     *
     * @param res 单车调教结果
     * @param u   用户身体数据
     * @param b   用户单车数据
     */
    private void initializeResult(StaticFittingResult res, UserBody u, UserBike b) {
        res.setFittingTime(LocalDateTime.now());
        res.setUserBodyId(u.getId());
        res.setUserBikeId(b.getId());
        res.setRiderTags(new ArrayList<>());
        res.setDetails(new ArrayList<>());
    }

    /**
     * 构建单车调教指标
     *
     * @param label     指标标签
     * @param ideal     理想值
     * @param current   当前值
     * @param tolerance 容差
     * @param reason    原因
     * @return 单车调教指标
     */
    private StaticFittingMetricVO buildMetric(String label, Number ideal, Number current, double tolerance, String reason) {
        return buildMetric(label, ideal, current, tolerance, reason, null);
    }

    private StaticFittingMetricVO buildMetric(String label, Number ideal, Number current, double tolerance, String reason, String forceStatus) {
        StaticFittingMetricVO metric = new StaticFittingMetricVO();
        metric.setLabel(label);
        metric.setUnit("mm");
        metric.setIdealValue(ideal);
        metric.setCurrentValue(current);
        metric.setReason(reason);
        double diff = ideal.doubleValue() - current.doubleValue();
        metric.setDelta(Math.round(diff * 10.0) / 10.0);
        if (forceStatus != null) {
            metric.setStatus(forceStatus);
            metric.setAction(forceStatus.equals("PERFECT") ? "保持现状" : (forceStatus.equals("ADJUST") ? "建议微调" : "需重装/更换"));
        } else {
            double absDiff = Math.abs(diff);
            if (absDiff <= tolerance) {
                metric.setStatus("PERFECT");
                metric.setAction("保持现状");
            } else if (absDiff <= tolerance * 2) {
                metric.setStatus("ADJUST");
                metric.setAction((diff > 0 ? "调高/长 " : "调低/短 ") + Math.round(absDiff) + "mm");
            } else {
                metric.setStatus("WARNING");
                metric.setAction("需重装/更换");
            }
        }
        return metric;
    }
}