package com.starrysky.aries.staticfitting.service;

import com.starrysky.aries.entity.*;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.staticfitting.repository.StaticFittingResultRepository;
import com.starrysky.aries.staticfitting.schema.*;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import com.starrysky.aries.userbike.repository.UserBikeSetupRepository;
import com.starrysky.aries.userbody.repository.UserBodyRepository;
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

    public Page<StaticFittingResultVO> getHistoryByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Object[]> dbPage = staticFittingResultRepository.findHistoryByUserId(userId, pageable);

        return dbPage.map(row -> {
            StaticFittingResult entity = (StaticFittingResult) row[0];
            String bikeName = (String) row[1];
            return convertToVO(entity, bikeName);
        });
    }

    public StaticFittingResultVO calculateStaticFitting(StaticFittingRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long bikeId = request.getUserBike() != null ? request.getUserBike().getId() : null;
        if (bikeId == null) {
            throw new BizException("进行诊断前请选择一辆实车或设定档");
        }
        List<RidingPainType> painLevels = request.getPainLevels();

        UserBody body = userBodyRepository.findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow(() -> new BizException("未找到身体测量数据，请先录入[身体图谱]"));

        if (body.getHeight() == null || body.getHeight() < 1200 || body.getHeight() > 2200 ||
                body.getSpanHeight() == null || body.getSpanHeight() < 600 || body.getSpanHeight() > 1000) {
            throw new BizException("基础数据(身高/跨高)疑似异常或缺失，请确认测量单位(mm)及数值准确性。");
        }

        UserBike bike = userBikeRepository.findById(bikeId).orElseThrow(() -> new BizException("找不到指定的单车信息"));
        UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId())
                .orElse(new UserBikeSetup());

        StaticFittingResult result = new StaticFittingResult();
        initializeResult(result, body, bike);

        try {
            processLevel1Sizing(body, setup, bike.getBikeModel().getBikeType(), result);
            processLevel2Ergonomics(user, body, bike.getBikeModel().getBikeType(), result);
            diagnosticsManager.diagnoseComponents(result, body);
            processLevel3Biomechanical(body, bike, setup, result);

            processBikeAdjustment(bike, setup, result);
            diagnosticsManager.addAdjustmentAdvice(result, bike, setup);

            if (painLevels != null && !painLevels.isEmpty()) {
                result.setInputPainTypes(new ArrayList<>(painLevels));
                processPainOverrides(result, painLevels);
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

    private StaticFittingResultVO convertToVO(StaticFittingResult entity, String bikeName) {
        StaticFittingResultVO vo = new StaticFittingResultVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        vo.setBikeName(bikeName != null ? bikeName : "纯人体测量基准");

        // 增加痛点文案映射
        if (entity.getInputPainTypes() != null && !entity.getInputPainTypes().isEmpty()) {
            vo.setPainLabels(entity.getInputPainTypes().stream()
                    .map(RidingPainType::getDescription)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    private void processLevel1Sizing(UserBody body, UserBikeSetup setup, int bikeType, StaticFittingResult res) {
        int baseHeight = StaticFittingCalculator.calcBaseSeatHeight(body.getSpanHeight(), bikeType);
        
        if (setup != null && setup.getPedalType() != null) {
            if (setup.getPedalType() == 1) baseHeight -= 10;
            else if (setup.getPedalType() == 3) baseHeight -= 3;
        }
        res.setRecSeatHeight(baseHeight);
        res.setRecStackPlus(StaticFittingCalculator.calcBaseStack(body.getSpanHeight(), bikeType));
        res.setRecCrankLength(StaticFittingCalculator.calcCrankLength(body.getSpanHeight(), bikeType));
        res.getRiderTags().add("核心基准已生成");
    }

    private void processLevel2Ergonomics(User user, UserBody body, int bikeType, StaticFittingResult res) {
        if (body.getIschialDistance() != null && body.getIschialDistance() > 0) {
            res.setRecSaddleWidth(StaticFittingCalculator.calcSaddleWidth(body.getIschialDistance(), bikeType));
        } else {
            // 男143/女155 默认值 fallback
            int defaultIschial = (user.getGender() != null && user.getGender() == 2) ? 155 : 143;
            res.setRecSaddleWidth(StaticFittingCalculator.calcSaddleWidth(defaultIschial, bikeType));
            addIncompleteNote(res, "器材匹配", "由于[坐骨间距]缺失，已按性别默认值比例给出座垫建议。");
        }

        if (body.getShoulderWidth() != null && body.getShoulderWidth() > 0) {
            int handleWidth = (bikeType == 1) ? (body.getShoulderWidth() / 10 * 10) : (body.getShoulderWidth() + 300);
            res.setRecHandlebarWidth(handleWidth);
        } else {
            // 男400/女380 默认值 fallback
            int defaultShoulder = (user.getGender() != null && user.getGender() == 2) ? 380 : 400;
            int handleWidth = (bikeType == 1) ? (defaultShoulder / 10 * 10) : (defaultShoulder + 300);
            res.setRecHandlebarWidth(handleWidth);
            addIncompleteNote(res, "器材匹配", "由于[肩宽]缺失，已按性别默认值比例给出把横宽度建议。");
        }
    }

    private void processLevel3Biomechanical(UserBody body, UserBike bike, UserBikeSetup setup, StaticFittingResult res) {

        Integer tScore = 0;
        if (body.getThoracicRotationAngle() != null && body.getThoracicExtensionAngle() != null) {
            int pain = (body.getThoracicPainLevel() != null) ? body.getThoracicPainLevel() : 0;
            tScore = (int) (body.getThoracicRotationAngle() * 0.6 + body.getThoracicExtensionAngle() * 0.4 - pain * 10);
            res.setThoracicScore(Math.max(0, tScore));
        }

        if (body.getThighLength() != null && body.getThighLength() > 0 && body.getSpanHeight() != null) {
            double femurRatio = (double) body.getThighLength() / body.getSpanHeight();
            int tibiaCorrection = StaticFittingCalculator.calcTibiaCorrection(body.getCalfLength() != null ? body.getCalfLength() : 0, body.getSpanHeight());
            
            res.setRecSeatHeight(res.getRecSeatHeight() + tibiaCorrection);
            res.setRecSetback(StaticFittingCalculator.calcSetback(body.getThighLength(), femurRatio));
        }

        if (body.getBreastHeight() != null && body.getLongArmLength() != null) {
            int trunk = body.getBreastHeight() - body.getSpanHeight();
            int arms = body.getLongArmLength() + (body.getShortArmLength() != null ? body.getShortArmLength() : body.getLongArmLength());

            res.setRecReachPlus((int) Math.round(StaticFittingCalculator.calcReachPlus(trunk, arms, tScore, bike.getBikeModel().getBikeType())));

            int h = (body.getHeight() != null) ? body.getHeight() : 1700;
            double ang = (body.getArmRaiseAngle() != null) ? body.getArmRaiseAngle().doubleValue() : 180.0;
            double drop = StaticFittingCalculator.calcHandlebarDrop(h, ang, tScore, bike.getBikeModel().getBikeType());
            res.setRecHandlebarDrop((int) Math.round(drop));
            res.setRecStackPlus(res.getRecSeatHeight() - (int) Math.round(drop));
        } else {
            addIncompleteNote(res, "上肢座舱", "由于[胸高]或[手臂长]缺失，无法计算 Reach+ / Stack+ 建议坐标。");
        }

        double femurRatio = (body.getThighLength() != null && body.getSpanHeight() != null) ? (double) body.getThighLength() / body.getSpanHeight() : 0.46;
        double apeIndex = (body.getLongArmLength() != null) ? (double) (body.getLongArmLength() * 2) / body.getHeight() : 1.0;
        
        diagnosticsManager.diagnosePortrait(res, body, apeIndex, femurRatio);
        diagnosticsManager.diagnoseLowerBody(res, femurRatio, (setup != null ? (Integer) setup.getSaddleHeight() : null));
        diagnosticsManager.diagnoseUpperBody(res, body);
    }

    private void processBikeAdjustment(UserBike bike, UserBikeSetup setup, StaticFittingResult res) {
        if (res.getRecReachPlus() == null || bike.getBikeModel() == null || bike.getBikeModel().getReach() == null) return;
        double stemRaw = res.getRecReachPlus() - bike.getBikeModel().getReach();
        if (setup == null || setup.getStemAngle() == null) {
            addIncompleteNote(res, "调教方案", "实车[把立角度]缺失，无法计算精确把立长度建议。");
            return;
        }
        double angleRad = Math.toRadians(Math.abs(setup.getStemAngle()));
        res.setRecStemLength((int) (Math.round((stemRaw / Math.cos(angleRad)) / 5.0) * 5));
    }

    private void processPainOverrides(StaticFittingResult res, List<RidingPainType> painLevels) {
        boolean hasAnterior = painLevels.contains(RidingPainType.KNEE_ANTERIOR);
        boolean hasPosterior = painLevels.contains(RidingPainType.KNEE_POSTERIOR);

        if (hasAnterior && hasPosterior) {
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("下肢动力链");
            d.setBodyFeature("矛盾疼痛点");
            d.setAdjustmentLogic("⚠️ 检测到您同时存在膝盖前后侧的矛盾痛感。这通常是由锁片位置严重错误或双腿肌力极度不平衡导致。请暂停调整高度！建议优先检查您的锁片是否对齐脚掌拇趾球，并进行针对性的肌肉放松。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        } else if (hasAnterior) {
            if (res.getRecSeatHeight() != null) res.setRecSeatHeight(res.getRecSeatHeight() + 8);
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("下肢动力链"); d.setBodyFeature("膝盖前侧痛");
            d.setAdjustmentLogic("🔴 【膝盖前侧痛】：因为处于痛感期，算法已将处方座垫高度提高 8mm。这通常是因为座垫过低导致踩踏上死点时膝盖弯曲过度，髌骨受压。调高后留意疼痛改善。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        } else if (hasPosterior) {
            if (res.getRecSeatHeight() != null) res.setRecSeatHeight(res.getRecSeatHeight() - 8);
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("下肢动力链"); d.setBodyFeature("膝盖后侧痛");
            d.setAdjustmentLogic("🔴 【膝盖后侧痛】：因为处于痛感期，算法已将处方座垫高度降低 8mm。过高的位置强迫膝盖在踩踏下死点时完全锁死过度拉伸了韧带。降低后留意疼痛改善。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.HAND_NUMBNESS)) {
            if (res.getRecReachPlus() != null) res.setRecReachPlus(res.getRecReachPlus() - 15);
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("上肢座舱"); d.setBodyFeature("手掌发麻/刺痛");
            d.setAdjustmentLogic("🔴 【手麻】：算法已将推荐前伸量缩短 15mm。您的重心可能过于前倾，压迫手腕神经。请考虑更换更短的把立。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.LOWER_BACK)) {
            if (res.getRecReachPlus() != null) res.setRecReachPlus(res.getRecReachPlus() - 10);
            if (res.getRecHandlebarDrop() != null) {
                res.setRecHandlebarDrop(res.getRecHandlebarDrop() - 10);
                if (res.getRecStackPlus() != null && res.getRecSeatHeight() != null) res.setRecStackPlus(res.getRecSeatHeight() - res.getRecHandlebarDrop());
            }
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("核心与腰背"); d.setBodyFeature("腰背酸痛");
            d.setAdjustmentLogic("🔴 【腰背痛】：算法已将推荐前伸量缩短并降低落差。您的核心可能无法支撑当前躯干延展，建议提高车把。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.NECK_PAIN)) {
            if (res.getRecReachPlus() != null) res.setRecReachPlus(res.getRecReachPlus() - 10);
            if (res.getRecHandlebarDrop() != null) res.setRecHandlebarDrop(res.getRecHandlebarDrop() - 10);
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("颈椎区域"); d.setBodyFeature("颈部酸痛/僵硬");
            d.setAdjustmentLogic("🔴 【颈部酸痛】：算法缩短了 10mm 前伸量并升高了车把。您的头部抬起角度过大。缩减前伸量可减少颈椎伸展负荷。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.SHOULDER_PAIN)) {
            if (res.getRecReachPlus() != null) res.setRecReachPlus(res.getRecReachPlus() - 5);
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("肩部区域"); d.setBodyFeature("肩部/斜方肌酸痛");
            d.setAdjustmentLogic("🔴 【肩部酸痛】：算法略微缩短了前伸量。请检查车把宽度是否过宽或过窄，这常会导致肩膀紧缩。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.ACHILLES_PAIN)) {
            if (res.getRecSeatHeight() != null) res.setRecSeatHeight(res.getRecSeatHeight() - 5);
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("脚踝动力链"); d.setBodyFeature("跟腱痛/足跟痛");
            d.setAdjustmentLogic("🔴 【跟腱痛】：算法降低了 5mm 座垫高度。过高的座垫会导致脚尖下勾严重，拉扯跟腱。建议同时检查锁片位置是否过于靠前。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.FOOT_NUMBNESS)) {
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("足底感知"); d.setBodyFeature("脚部发麻/灼热");
            d.setAdjustmentLogic("⚠️ 【脚麻】：该症状通常由于锁鞋过窄或锁片中心点错误（过前）导致压迫。建议检查锁片中心是否对齐大脚趾拇趾球，并微调锁片略向后移。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.KNEE_LATERAL)) {
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("膝盖侧向"); d.setBodyFeature("膝盖外侧痛");
            d.setAdjustmentLogic("⚠️ 【膝外侧痛】：可能涉及瓣膜或髂胫束压迫。建议检查锁片旋转角度（Float）是否不足，或尝试减小 Q-Factor（曲柄宽度/脚踏站间距）。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }

        if (painLevels.contains(RidingPainType.KNEE_MEDIAL)) {
            DiagnosticDetail d = new DiagnosticDetail();
            d.setPart("膝盖侧向"); d.setBodyFeature("膝盖内侧痛");
            d.setAdjustmentLogic("⚠️ 【膝内侧痛】：通常由于站间距过窄。建议尝试增加脚踏垫片（Pedal Spacers）或向内移动锁片以增加 Q-Factor。");
            res.getDetails().add(d);
            d.setStaticFittingResult(res);
        }
    }

    public StaticFittingReportVO diagnoseBikeSetup(Long userId, Long userBikeId) {
        UserBody body = userBodyRepository.findTopByUserIdOrderByIdDesc(userId).orElseThrow(() -> new BizException("身体数据不存在"));
        UserBike bike = userBikeRepository.findById(userBikeId).orElseThrow(() -> new BizException("车辆数据不存在"));
        UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId()).orElse(new UserBikeSetup());

        StaticFittingResult ideal = staticFittingResultRepository.findTopByUserBodyIdAndUserBikeIdOrderByFittingTimeDesc(body.getId(), bike.getId())
                .orElseThrow(() -> new BizException("请先生成当前车辆的 Fitting 方案"));

        StaticFittingReportVO report = new StaticFittingReportVO();
        report.setFittingResultId("FIT-" + ideal.getId());
        report.setCreateTime(ideal.getFittingTime().toString());

        List<StaticFittingMetricVO> mechanicalAdjustments = new ArrayList<>();
        List<String> expertDiagnoses = new ArrayList<>();
        List<HardwareUpgradeDTO> hardwareUpgrades = new ArrayList<>();
        double maxDelta = 0;

        if (ideal.getRecSeatHeight() != null && setup.getSaddleHeight() != null) {
            StaticFittingMetricVO m = buildMetric("Saddle Height (座垫高度)", "mm", ideal.getRecSeatHeight(), setup.getSaddleHeight(), 5, "决定踩踏效率与膝盖健康");
            mechanicalAdjustments.add(m);
            maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().intValue()));
            if (Math.abs(m.getDelta().intValue()) > 20) expertDiagnoses.add(m.getDelta().intValue() > 0 ? "🔴 座垫显著偏低，易造成膝前受压。" : "🔴 座垫显著偏高，易造成大腿后侧拉伤。");
        }

        if (ideal.getRecReachPlus() != null && bike.getBikeModel() != null && bike.getBikeModel().getReach() != null && setup.getStemLength() != null) {
            double currentReachPlus = calculateCurrentReachPlus(bike.getBikeModel(), setup);
            StaticFittingMetricVO m = buildMetric("Reach+ (有效前伸量)", "mm", ideal.getRecReachPlus(), currentReachPlus, 10, "影响重心分布与操控舒适度");
            mechanicalAdjustments.add(m);
            maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().intValue()));
            if (Math.abs(m.getDelta().intValue()) > 25) expertDiagnoses.add("🔴 骑行姿态过" + (m.getDelta().intValue() > 0 ? "短" : "长") + "，建议调整把立长度。");
        }

        if (ideal.getRecCrankLength() != null && setup.getCrankLength() != null && !ideal.getRecCrankLength().equals(setup.getCrankLength())) {
            HardwareUpgradeDTO h = new HardwareUpgradeDTO(); h.setPart("曲柄长度");
            h.setCurrentValue(setup.getCrankLength() + "mm"); h.setTargetSpec(ideal.getRecCrankLength() + "mm");
            h.setReason("您的跨高对应的最佳曲柄杠杆比为" + ideal.getRecCrankLength() + "mm。");
            hardwareUpgrades.add(h);
        }

        report.setMechanicalAdjustments(mechanicalAdjustments);
        report.setExpertDiagnoses(expertDiagnoses);
        report.setHardwareUpgrades(hardwareUpgrades);
        
        if (maxDelta > 40) {
            report.setOverallStatus("DANGER"); report.setMatchLevel("偏差极大");
            report.setSummary("🚨 警告：当前设定与基准偏离超过 40mm，切勿盲目调整，建议核实测量数据或车架尺码。");
        } else if (maxDelta > 10) {
            report.setOverallStatus("WARNING"); report.setMatchLevel("需优化");
            report.setSummary("检测到关键参数存在明显偏差。");
        } else {
            report.setOverallStatus("PASS"); report.setMatchLevel("完美契合");
            report.setSummary("当前设定非常理想，尽情享受骑行！");
        }
        return report;
    }

    private void addIncompleteNote(StaticFittingResult res, String part, String logic) {
        DiagnosticDetail detail = new DiagnosticDetail();
        detail.setPart(part); detail.setAdjustmentLogic("【数据缺失】" + logic);
        res.getDetails().add(detail);
        detail.setStaticFittingResult(res);
    }

    private double calculateCurrentReachPlus(BikeModel model, UserBikeSetup setup) {
        return model.getReach() + (setup.getStemLength() * Math.cos(Math.toRadians(Math.abs(setup.getStemAngle()))));
    }

    private void assembleFinalPostureSuggestion(StaticFittingResult res) {
        String fullReport = res.getDetails().stream()
                .map(d -> "【" + d.getPart() + "】" + d.getAdjustmentLogic())
                .collect(Collectors.joining("\n\n"));
        res.setPostureSuggestion(fullReport);
    }

    private void initializeResult(StaticFittingResult res, UserBody u, UserBike b) {
        res.setFittingTime(LocalDateTime.now());
        res.setUserBodyId(u.getId());
        res.setUserBikeId(b.getId());
        res.setRiderTags(new ArrayList<>());
        res.setDetails(new ArrayList<>());
    }

    private StaticFittingMetricVO buildMetric(String label, String unit, Number ideal, Number current, double tolerance, String reason) {
        StaticFittingMetricVO metric = new StaticFittingMetricVO();
        metric.setLabel(label); metric.setUnit(unit); metric.setIdealValue(ideal); metric.setCurrentValue(current); metric.setReason(reason);
        double diff = ideal.doubleValue() - current.doubleValue();
        metric.setDelta(Math.round(diff * 10.0) / 10.0);
        double absDiff = Math.abs(diff);
        if (absDiff <= tolerance) {
            metric.setStatus("PERFECT"); metric.setAction("保持现状");
        } else if (absDiff <= tolerance * 2) {
            metric.setStatus("ADJUST");
            metric.setAction((diff > 0 ? "调高/长 " : "调低/短 ") + Math.round(absDiff) + unit);
        } else {
            metric.setStatus("WARNING"); metric.setAction("需重装/更换");
        }
        return metric;
    }
}