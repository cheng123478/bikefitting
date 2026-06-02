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
    private static final int MIN_STEM_LENGTH = 40;
    private static final int MAX_STEM_LENGTH = 160;
    private static final int MIN_SPACER_HEIGHT = 0;
    private static final int MAX_SPACER_HEIGHT = 80;
    private static final int COCKPIT_STEP = 5;
    private static final int[] COMMON_STEM_ANGLES = {-17, -12, -10, -8, -7, -6, 0, 6, 7, 10, 17};

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
     * 获取用户的静态Fitting报告总数
     */
    public long countByUserId(Long userId) {
        return staticFittingResultRepository.countByUserId(userId);
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
            int validationCode = getValidationCode(body, user);
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
            applyAbdominalClearanceProtocol(body, result);
            if (result.getRiderTags().contains("腹部空间优化") || result.getRiderTags().contains("腹部空间优化-增强")) {
                removeUpperBodyPostureDetail(result);
                diagnosticsManager.diagnoseUpperBody(result, body, setup);
            }

            // B-2. 痛点修正 (在计算把立和垫圈的机械调校处方之前执行，确保调教推荐直接响应痛点保护)
            List<RidingPainType> painLevels = request.getPainLevels();
            if (painLevels != null && !painLevels.isEmpty()) {
                result.setInputPainTypes(new ArrayList<>(painLevels));
                diagnosticsManager.diagnosePain(result, painLevels);
                normalizePostureTargets(result);
                refreshPrimaryPostureDetails(result, body, setup);
            }

            // C. 现状对比与器材调教处方
            processBikeAdjustment(bike, result);
            diagnosticsManager.addAdjustmentAdvice(result, setup);

            diagnosticsManager.diagnoseSafety(result, body);
            assembleFinalPostureSuggestion(result);

        } catch (Exception e) {
            log.error("Fitting Engine Error: ", e);
            result.setPostureSuggestion("计算过程中由于部分数据异常导致中断: " + e.getMessage());
        }

        StaticFittingResult savedResult = staticFittingResultRepository.save(result);
        return convertToVO(savedResult, bike.getBikeModel() != null ? bike.getBikeModel().getModel() : "未知车型");
    }

    private static int getValidationCode(UserBody body, User user) {
        Integer wingspan = null;
        if (body.getLongArmLength() != null) {
            int singleArm = body.getLongArmLength() + (body.getShortArmLength() != null ? body.getShortArmLength() : body.getLongArmLength());
            int gender = (user.getGender() != null) ? user.getGender() : 1;
            double shoulderEst = body.getShoulderWidth() != null ? body.getShoulderWidth() : (body.getHeight() * (gender == 2 ? 0.21 : 0.22));
            wingspan = (int) Math.round(singleArm * 2 + shoulderEst);
        }
        return StaticFittingCalculator.validateHumanProportions(body.getSpanHeight(), body.getHeight(), wingspan);
    }

    private void validateCoreData(UserBody body) {
        if (body.getHeight() == null || body.getHeight() < 1200 || body.getHeight() > 2200 ||
                body.getSpanHeight() == null || body.getSpanHeight() < 600 || body.getSpanHeight() > 1100) {
            throw new BizException("基础数据(身高/跨高)疑似异常或缺失，请确认测量单位(mm)及数值准确性。");
        }
        if (body.getWeightKg() == null || body.getWeightKg() < 30 || body.getWeightKg() > 250) {
            throw new BizException("体重为必填项，请确认单位为 kg，且数值在 30-250kg 的合理范围内。");
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
            // 男性坐骨间距平均值 118mm，女性平均值 130mm
            int defaultIschial = (user.getGender() != null && user.getGender() == 2) ? 130 : 118;
            res.setRecSaddleWidth(StaticFittingCalculator.calcSaddleWidth(defaultIschial, bikeType));
            addIncompleteNote(res, "器材匹配", "missing_ischial");
        }

        // 区分弯把系统（公路车、全地形公路车、铁三车）与平把系统（山地车、城市通勤车）的宽度推荐
        boolean isDropBar = (bikeType == BikeType.ROAD_BIKE.code || bikeType == BikeType.GRAVEL_BIKE.code || bikeType == BikeType.TT_BIKE.code);
        int shoulder = body.getShoulderWidth() != null ? body.getShoulderWidth() : ((user.getGender() != null && user.getGender() == 2) ? 380 : 400);
        int handleWidth;
        if (isDropBar) {
            int baseWidth = shoulder / 10 * 10;
            if (bikeType == BikeType.GRAVEL_BIKE.code) {
                handleWidth = baseWidth + 20; // 越野路况微宽，以增加控制力
            } else {
                handleWidth = baseWidth;
            }
        } else {
            handleWidth = shoulder + 300; // 平把（山地/城市车）提供杠杆支撑
        }
        res.setRecHandlebarWidth(handleWidth);
        if (body.getShoulderWidth() == null || body.getShoulderWidth() <= 0) {
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

        int tScore = 60;
        if (body.getThoracicRotationAngle() != null && body.getArmRaiseAngle() != null) {
            int pain = (body.getThoracicPainLevel() != null) ? body.getThoracicPainLevel() : 0;
            tScore = (int) (body.getThoracicRotationAngle() * 0.6 + body.getArmRaiseAngle() * 0.4 - pain * 10);
            tScore = Math.min(100, Math.max(0, tScore)); // 保证在百分制合理范围内
            res.setThoracicScore(tScore);
        } else {
            addIncompleteNote(res, "上肢座舱", "missing_mobility");
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
        
        // 计算真实且自洽的翼展和猿臂比
        double apeIndex = 1.0;
        if (body.getLongArmLength() != null && body.getHeight() != null) {
            int singleArm = body.getLongArmLength() + (body.getShortArmLength() != null ? body.getShortArmLength() : body.getLongArmLength());
            int gender = (body.getUser() != null && body.getUser().getGender() != null) ? body.getUser().getGender() : 1;
            double shoulderEst = body.getShoulderWidth() != null ? body.getShoulderWidth() : (body.getHeight() * (gender == 2 ? 0.21 : 0.22));
            apeIndex = (singleArm * 2 + shoulderEst) / body.getHeight();
        }

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
    private void applyAbdominalClearanceProtocol(UserBody body, StaticFittingResult res) {
        Double bmi = StaticFittingCalculator.calculateBmi(body.getHeight(), body.getWeightKg());
        Double waistToHeightRatio = StaticFittingCalculator.calculateWaistToHeightRatio(body.getHeight(), body.getWaistCircumference());
        if (bmi == null) {
            return;
        }
        if (waistToHeightRatio == null) {
            if (bmi >= FittingConstants.BMI_OBESITY_THRESHOLD) {
                res.getRiderTags().add("腹部空间待确认");
                diagnosticsManager.addDetail(
                        res,
                        "上肢座舱",
                        "腹部空间待确认",
                        "体重与身高提示可能存在腹部空间需求，但本次未填写腰围，无法判断腰高比。系统保留常规静态 fitting 结果，未自动启用腹部空间优化；如骑行时有大腿顶腹、腹部压迫或深把位呼吸受限，建议补录腰围后重新生成方案。"
                );
            }
            return;
        }
        if (!StaticFittingCalculator.hasAbdominalClearanceNeed(bmi, waistToHeightRatio)) {
            return;
        }
        if (res.getRecReachPlus() == null || res.getRecHandlebarDrop() == null || res.getRecSeatHeight() == null) {
            return;
        }

        int reachReduction = StaticFittingCalculator.calcAbdominalReachReduction(bmi, waistToHeightRatio);
        int dropReduction = StaticFittingCalculator.calcAbdominalDropReduction(bmi, waistToHeightRatio);
        res.setRecReachPlus(Math.max(res.getRecReachPlus() - reachReduction, 350));
        res.setRecHandlebarDrop(clampHandlebarDrop(res.getRecHandlebarDrop() - dropReduction));
        refreshTargetBarHeight(res);

        if (StaticFittingCalculator.isSevereAbdominalClearanceNeed(waistToHeightRatio)) {
            res.getRiderTags().add("腹部空间优化-增强");
        } else {
            res.getRiderTags().add("腹部空间优化");
        }
    }

    private void processBikeAdjustment(UserBike bike, StaticFittingResult res) {
        if (res.getRecReachPlus() == null || res.getTargetBarHeight() == null) {
            return;
        }
        BikeModel bikeModel = bike.getBikeModel();
        if (bikeModel == null || bikeModel.getReach() == null || bikeModel.getStack() == null) {
            addIncompleteNote(res, "调教方案", "missing_frame_geometry");
            return;
        }

        CockpitFitCandidate fit = findBestCockpitFit(
                res.getRecReachPlus(),
                res.getTargetBarHeight(),
                bikeModel.getReach(),
                bikeModel.getStack(),
                bikeModel.getBikeType()
        );
        if (fit != null) {
            res.setRecStemLength(fit.stemLength);
            res.setRecStemAngle(fit.stemAngle);
            res.setRecSpacerHeight(fit.spacerHeight);
        }
    }

    private CockpitFitCandidate findBestCockpitFit(int targetReachPlus, int targetBarHeight, int frameReach,
                                                   int frameStack, Integer bikeType) {
        CockpitFitCandidate best = null;
        for (int stemLength = MIN_STEM_LENGTH; stemLength <= MAX_STEM_LENGTH; stemLength += COCKPIT_STEP) {
            for (int angle : COMMON_STEM_ANGLES) {
                double angleRad = Math.toRadians(angle);
                double reach = frameReach + stemLength * Math.cos(Math.abs(angleRad));
                double barHeightWithoutSpacer = frameStack + stemLength * Math.sin(angleRad);
                for (int spacer = MIN_SPACER_HEIGHT; spacer <= MAX_SPACER_HEIGHT; spacer += COCKPIT_STEP) {
                    double barHeight = barHeightWithoutSpacer + spacer;
                    double reachError = Math.abs(targetReachPlus - reach);
                    double heightError = Math.abs(targetBarHeight - barHeight);
                    double score = reachError * 1.15 + heightError + cockpitHardwarePenalty(stemLength, angle, spacer, bikeType);

                    if (best == null || score < best.score) {
                        best = new CockpitFitCandidate(stemLength, (double) angle, spacer, score);
                    }
                }
            }
        }
        return best;
    }

    private double cockpitHardwarePenalty(int stemLength, int stemAngle, int spacerHeight, Integer bikeType) {
        double penalty = 0;
        int preferredMin = 60;
        int preferredMax = 120;
        if (Objects.equals(bikeType, BikeType.MOUNTAIN_BIKE.code) || Objects.equals(bikeType, BikeType.CITY_BIKE.code)) {
            preferredMin = 40;
            preferredMax = 100;
        } else if (Objects.equals(bikeType, BikeType.TT_BIKE.code)) {
            preferredMin = 70;
            preferredMax = 130;
        }

        if (stemLength < preferredMin) {
            penalty += (preferredMin - stemLength) * 0.25;
        } else if (stemLength > preferredMax) {
            penalty += (stemLength - preferredMax) * 0.25;
        }

        if (spacerHeight > 40) {
            penalty += (spacerHeight - 40) * 0.35;
        }
        if (spacerHeight > 60) {
            penalty += (spacerHeight - 60) * 0.55;
        }

        if (Math.abs(stemAngle) == 6 || Math.abs(stemAngle) == 7) {
            return penalty;
        }
        if (stemAngle == 0) {
            return penalty + 0.5;
        }
        if (Math.abs(stemAngle) <= 10) {
            return penalty + 1.0;
        }
        if (Math.abs(stemAngle) <= 12) {
            return penalty + 1.5;
        }
        return penalty + 2.5;
    }

    private static class CockpitFitCandidate {
        private final int stemLength;
        private final double stemAngle;
        private final int spacerHeight;
        private final double score;

        private CockpitFitCandidate(int stemLength, double stemAngle, int spacerHeight, double score) {
            this.stemLength = stemLength;
            this.stemAngle = stemAngle;
            this.spacerHeight = spacerHeight;
            this.score = score;
        }
    }

    /**
     * 诊断单车调教 (按单车ID查询最新报告)
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

        return buildReportFromIdeal(ideal, bike, setup);
    }

    /**
     * 根据结果ID诊断单车调教 (用于分享和历史详情查询)
     *
     * @param userId   用户ID
     * @param resultId 静态Fitting结果ID
     * @return 单车调教报告
     */
    public StaticFittingReportVO diagnoseBikeSetupByResultId(Long userId, Long resultId) {
        StaticFittingResult ideal = staticFittingResultRepository.findById(resultId)
                .orElseThrow(() -> new BizException("未找到指定的 Fitting 方案"));

        // 安全校验：确认报告的所有权
        UserBody body = userBodyRepository.findById(ideal.getUserBodyId())
                .orElseThrow(() -> new BizException("身体数据不存在"));
        if (!body.getUser().getId().equals(userId)) {
            throw new BizException("无权查看该 Fitting 报告");
        }

        UserBike bike = userBikeRepository.findById(ideal.getUserBikeId())
                .orElseThrow(() -> new BizException("车辆数据不存在"));
        UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId()).orElse(new UserBikeSetup());

        return buildReportFromIdeal(ideal, bike, setup);
    }

    /**
     * 核心报告组装与综合打分引擎
     */
    private StaticFittingReportVO buildReportFromIdeal(StaticFittingResult ideal, UserBike bike, UserBikeSetup setup) {
        StaticFittingReportVO report = new StaticFittingReportVO();
        report.setFittingResultId("FIT-" + ideal.getId());
        report.setCreateTime(ideal.getFittingTime().toString());

        // 车架几何适用性：Reach+ 检查的是“推荐座舱是否可落地”，不是把车架 Reach 直接和 Reach+ 相比。
        if (bike.getBikeModel() != null) {
            if (ideal.getRecFrameStack() != null && bike.getBikeModel().getStack() != null) {
                int stackDelta = Math.abs(ideal.getRecFrameStack() - bike.getBikeModel().getStack());
                String stackStatus = stackDelta <= 10 ? "PERFECT" : (stackDelta <= 25 ? "ADJUST" : "WARNING");
                String stackReason = stackDelta <= 10 ? "车架 Stack 接近静态推荐区间，车头高度有较好调整余量" :
                    (stackDelta <= 25 ? "车架 Stack 与推荐区间有差异，优先通过垫圈和把立规格微调" : "车架 Stack 与推荐区间差异较大，后续车头高度调整余量可能受限");
                report.setFrameStackCheck(buildMetric("Frame Stack (车架 Stack)", ideal.getRecFrameStack(), bike.getBikeModel().getStack(), 10, stackReason, stackStatus));
            }
            if (ideal.getRecReachPlus() != null && bike.getBikeModel().getReach() != null
                    && ideal.getRecStemLength() != null && ideal.getRecStemAngle() != null) {
                double reachableReachPlus = bike.getBikeModel().getReach()
                        + StaticFittingCalculator.calculateReachOffset(ideal.getRecStemLength(), ideal.getRecStemAngle());
                double reachDelta = Math.abs(ideal.getRecReachPlus() - reachableReachPlus);
                String reachStatus = reachDelta <= 8 ? "PERFECT" : (reachDelta <= 18 ? "ADJUST" : "WARNING");
                String reachReason = reachDelta <= 8 ? "当前车架搭配推荐把立长度后，可接近目标 Reach+ 坐标" :
                    (reachDelta <= 18 ? "当前车架可接近目标 Reach+，但需要结合把立长度、座垫前后和手变头位置微调" : "按常见把立范围仍难以接近目标 Reach+，说明车架前后几何可能受限");
                report.setFrameReachCheck(buildMetric("Reach+ 可达性", ideal.getRecReachPlus(), Math.round(reachableReachPlus * 10.0) / 10.0, 8, reachReason, reachStatus));
            } else if (ideal.getRecReachPlus() != null && bike.getBikeModel().getReach() != null) {
                report.setFrameReachCheck(buildReferenceMetric("Reach+ 可达性", ideal.getRecReachPlus(), "缺少完整车架几何或座舱推荐，暂不判断当前车架是否能落到目标 Reach+。"));
            }
        }

        List<StaticFittingMetricVO> mechanicalAdjustments = new ArrayList<>();
        List<String> expertDiagnoses = new ArrayList<>();
        List<HardwareUpgradeVO> hardwareUpgrades = new ArrayList<>();
        
        double maxDelta = 0;
        double totalWeightedScore = 0;
        double totalWeight = 0;
        int comparisonCount = 0;

        // 1. 座垫高度
        if (ideal.getRecSeatHeight() != null) {
            if (setup.getSaddleHeight() != null) {
                StaticFittingMetricVO m = buildMetric("Saddle Height (座垫高度)", ideal.getRecSeatHeight(), setup.getSaddleHeight(), 5, "决定踩踏效率、膝关节伸展和骨盆稳定");
                mechanicalAdjustments.add(m);
                double delta = Math.abs(m.getDelta().doubleValue());
                maxDelta = Math.max(maxDelta, delta);
                if (delta > 20) {
                    expertDiagnoses.add(m.getDelta().doubleValue() > 0 ? "当前座高明显偏低，可能增加膝前压力；建议先小幅上调并观察膝盖反馈。" : "当前座高明显偏高，可能带来骨盆晃动、腘绳肌或跟腱压力；建议先小幅下调。");
                }
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 5) * 0.30;
                totalWeight += 0.30;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Saddle Height (座垫高度)", ideal.getRecSeatHeight(), "先按推荐座高作为静态起点，再结合动态膝角、骨盆稳定和疼痛反馈微调。"));
            }
        }

        // 2. 有效前伸量
        if (ideal.getRecReachPlus() != null) {
            if (bike.getBikeModel() != null && bike.getBikeModel().getReach() != null
                    && setup.getStemLength() != null && setup.getStemAngle() != null) {
                double currentReachPlus = calculateCurrentReachPlus(bike.getBikeModel(), setup);
                StaticFittingMetricVO m = buildMetric("Reach+ (有效前伸量)", ideal.getRecReachPlus(), currentReachPlus, 10, "影响上肢承重、重心分布和操控稳定");
                mechanicalAdjustments.add(m);
                double delta = Math.abs(m.getDelta().doubleValue());
                maxDelta = Math.max(maxDelta, delta);
                if (delta > 25) {
                    expertDiagnoses.add("当前前伸量明显偏" + (m.getDelta().doubleValue() > 0 ? "短" : "长") + "；优先检查把立长度，再看座垫前后和手变头位置。");
                }
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 10) * 0.25;
                totalWeight += 0.25;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Reach+ (有效前伸量)", ideal.getRecReachPlus(), "这是身体侧目标坐标；未填写当前把立长度与角度时，不计算当前 setup 偏差。"));
            }
        }

        // 3. 坐垫后移
        if (ideal.getRecSetback() != null) {
            if (setup.getSaddleSetback() != null) {
                StaticFittingMetricVO m = buildMetric("Setback (座垫后移)", ideal.getRecSetback(), setup.getSaddleSetback(), 5, "影响踩踏重心、髋膝角度和臀肌介入");
                mechanicalAdjustments.add(m);
                maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().doubleValue()));
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 5) * 0.15;
                totalWeight += 0.15;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Setback (座垫后移)", ideal.getRecSetback(), "先按推荐后移量建立发力重心，再用膝盖压力和坐垫支撑感微调。"));
            }
        }

        // 4. 曲柄长度
        if (ideal.getRecCrankLength() != null) {
            if (setup.getCrankLength() != null) {
                StaticFittingMetricVO m = buildMetric("Crank Length (曲柄长度)", ideal.getRecCrankLength(), setup.getCrankLength(), 2.5, "影响髋膝活动范围、踏频习惯和低趴时的髋腹空间");
                mechanicalAdjustments.add(m);
                maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().doubleValue()));
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 2.5) * 0.08;
                totalWeight += 0.08;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Crank Length (曲柄长度)", ideal.getRecCrankLength(), "曲柄属于器材规格项，不需要立刻更换；下次升级或装车时按该规格优先筛选。"));
            }
        }

        // 5. 坐垫推荐宽
        if (ideal.getRecSaddleWidth() != null) {
            if (setup.getSaddleWidth() != null) {
                StaticFittingMetricVO m = buildMetric("Saddle Width (坐垫推荐宽)", ideal.getRecSaddleWidth(), setup.getSaddleWidth(), 5, "影响坐骨支撑、会阴压力和长距离舒适度");
                mechanicalAdjustments.add(m);
                maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().doubleValue()));
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 5) * 0.06;
                totalWeight += 0.06;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Saddle Width (坐垫推荐宽)", ideal.getRecSaddleWidth(), "按坐骨间距选择座垫宽度，优先保证坐骨支撑而不是单纯追求窄或轻。"));
            }
        }

        // 6. 车把落差
        if (ideal.getRecHandlebarDrop() != null) {
            if (setup.getHandlebarDrop() != null) {
                StaticFittingMetricVO m = buildMetric("Handlebar Drop (车把落差)", ideal.getRecHandlebarDrop(), setup.getHandlebarDrop(), 10, "影响躯干角度、肩颈压力、视野和风阻");
                mechanicalAdjustments.add(m);
                maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().doubleValue()));
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 10) * 0.10;
                totalWeight += 0.10;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Handlebar Drop (车把落差)", ideal.getRecHandlebarDrop(), "把落差作为舒适与效率的起点；如果腰背、肩颈或手掌压力明显，优先降低落差。"));
            }
        }

        // 7. 垫圈高度
        if (ideal.getRecSpacerHeight() != null) {
            if (setup.getSpacersHeight() != null) {
                StaticFittingMetricVO m = buildMetric("Spacer Height (垫圈高度)", ideal.getRecSpacerHeight(), setup.getSpacersHeight(), 5, "影响车把高度，是最容易落地的车头微调项");
                mechanicalAdjustments.add(m);
                maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().doubleValue()));
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 5) * 0.06;
                totalWeight += 0.06;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Spacer Height (垫圈高度)", ideal.getRecSpacerHeight(), "这是按车架几何和目标车把高度反推的垫圈建议，方便直接调车。"));
            }
        }

        // 8. 把立长度
        if (ideal.getRecStemLength() != null) {
            if (setup.getStemLength() != null) {
                StaticFittingMetricVO m = buildMetric("Stem Length (把立长度)", ideal.getRecStemLength(), setup.getStemLength(), 5, "影响前伸量、上肢承重和转向响应");
                mechanicalAdjustments.add(m);
                maxDelta = Math.max(maxDelta, Math.abs(m.getDelta().doubleValue()));
                totalWeightedScore += calculateItemScore(m.getDelta().doubleValue(), 5) * 0.06;
                totalWeight += 0.06;
                comparisonCount++;
            } else {
                mechanicalAdjustments.add(buildReferenceMetric("Stem Length (把立长度)", ideal.getRecStemLength(), "把立长度是实现目标 Reach+ 的主要可更换零件；优先按推荐长度选择常见规格。"));
            }
        }

        if (ideal.getRecCrankLength() != null && setup.getCrankLength() != null
                && Math.abs(ideal.getRecCrankLength() - setup.getCrankLength()) >= 1.0) {
            HardwareUpgradeVO h = new HardwareUpgradeVO();
            h.setPart("曲柄长度");
            h.setCurrentValue(setup.getCrankLength() + "mm");
            h.setTargetSpec(formatMillimeter(ideal.getRecCrankLength()));
            h.setReason("您的跨高与车型对应的曲柄静态建议为 " + formatMillimeter(ideal.getRecCrankLength()) + "；下次更换传动或曲柄时可优先参考。");
            hardwareUpgrades.add(h);
        }

        if (ideal.getRiderTags() != null) {
            if (ideal.getRiderTags().contains("腹部空间优化-增强")) {
                expertDiagnoses.add("系统已启用增强版腹部空间优化：缩短前伸并抬高把位，优先保留髋腹夹角与呼吸空间。");
            } else if (ideal.getRiderTags().contains("腹部空间优化")) {
                expertDiagnoses.add("系统已启用腹部空间优化：适度缩短前伸并降低落差，减少大腿顶腹、腹部压迫或深把位呼吸受限。");
            } else if (ideal.getRiderTags().contains("腹部空间待确认")) {
                expertDiagnoses.add("体重与身高提示可能存在腹部空间需求，但腰围未填写；本次未自动启用腹部空间优化，建议补录腰围后重新生成方案。");
            }
        }

        report.setMechanicalAdjustments(mechanicalAdjustments);
        report.setExpertDiagnoses(expertDiagnoses);
        report.setHardwareUpgrades(hardwareUpgrades);

        // 计算匹配得分
        Integer matchScore = null;
        if (totalWeight > 0) {
            matchScore = (int) Math.round(totalWeightedScore / totalWeight);
        }
        report.setMatchScore(matchScore);

        boolean hasReferenceOnly = mechanicalAdjustments.stream().anyMatch(m -> "REFERENCE".equals(m.getStatus()));
        if (matchScore == null) {
            report.setOverallStatus("REFERENCE");
            report.setMatchLevel("仅推荐");
            report.setSummary("已根据您的身体数据和车架几何生成静态推荐值；由于当前实车设定未填写，本报告不评价“差多少”，只提供可直接调车的目标。");
        } else if (maxDelta > 40 || matchScore < 60) {
            report.setOverallStatus("DANGER");
            report.setMatchLevel("偏差极大");
            report.setSummary("已填写的设定与静态推荐差异较大。建议先从座高、后移、把立长度和垫圈高度这些可控项开始，逐步调整并观察膝盖、腰背、肩颈和手部反馈。");
        } else if (maxDelta > 10 || matchScore < 85) {
            report.setOverallStatus("WARNING");
            report.setMatchLevel("需优化");
            report.setSummary("已填写的关键参数存在可优化空间。优先调整偏差最大的项目，单次调整幅度保持保守，并通过短距离试骑验证。");
        } else if (hasReferenceOnly || comparisonCount < 5) {
            report.setOverallStatus("PARTIAL");
            report.setMatchLevel("部分对比");
            report.setSummary("已填写的项目整体接近推荐值；未填写的当前设定仅展示推荐目标，不参与通过判定。");
        } else {
            report.setOverallStatus("PASS");
            report.setMatchLevel("匹配良好");
            report.setSummary("已填写的实车设定整体接近静态推荐。建议保留当前方向，并通过 2-3 次短距离骑行确认身体反馈。");
        }
        return report;
    }

    /**
     * 单原子项匹配度得分计算公式
     */
    private double calculateItemScore(double diff, double tolerance) {
        double absDiff = Math.abs(diff);
        if (absDiff <= tolerance) {
            return 100.0;
        }
        double score = 100.0 - ((absDiff - tolerance) / (2.0 * tolerance)) * 100.0;
        return Math.max(0.0, score);
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

    private void normalizePostureTargets(StaticFittingResult res) {
        if (res.getRecReachPlus() != null) {
            res.setRecReachPlus(Math.max(res.getRecReachPlus(), 350));
        }
        if (res.getRecHandlebarDrop() != null) {
            res.setRecHandlebarDrop(clampHandlebarDrop(res.getRecHandlebarDrop()));
        }
        refreshTargetBarHeight(res);
    }

    private void refreshTargetBarHeight(StaticFittingResult res) {
        if (res.getRecSeatHeight() != null && res.getRecHandlebarDrop() != null) {
            res.setTargetBarHeight(res.getRecSeatHeight() - res.getRecHandlebarDrop());
        }
    }

    private int clampHandlebarDrop(int drop) {
        return Math.max(-80, Math.min(160, drop));
    }

    private void refreshPrimaryPostureDetails(StaticFittingResult res, UserBody body, UserBikeSetup setup) {
        removePrimaryPostureDetails(res);
        diagnosticsManager.diagnoseLowerBody(res, calculateFemurRatio(body), setup);
        diagnosticsManager.diagnoseUpperBody(res, body, setup);
    }

    private void removePrimaryPostureDetails(StaticFittingResult res) {
        res.getDetails().removeIf(d ->
                ("下肢动力链".equals(d.getPart()) && "踩踏力线".equals(d.getBodyFeature()))
                        || ("上肢座舱".equals(d.getPart()) && "舒适度/落差".equals(d.getBodyFeature()))
        );
    }

    private void removeUpperBodyPostureDetail(StaticFittingResult res) {
        res.getDetails().removeIf(d -> "上肢座舱".equals(d.getPart()) && "舒适度/落差".equals(d.getBodyFeature()));
    }

    private double calculateFemurRatio(UserBody body) {
        return (body.getThighLength() != null && body.getSpanHeight() != null)
                ? (double) body.getThighLength() / body.getSpanHeight()
                : 0.46;
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

    private StaticFittingMetricVO buildReferenceMetric(String label, Number ideal, String reason) {
        StaticFittingMetricVO metric = new StaticFittingMetricVO();
        metric.setLabel(label);
        metric.setUnit("mm");
        metric.setIdealValue(ideal);
        metric.setCurrentValue(null);
        metric.setDelta(null);
        metric.setStatus("REFERENCE");
        metric.setAction("按推荐值调车");
        metric.setReason(reason);
        return metric;
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
            metric.setAction(forceStatus.equals("PERFECT") ? "当前可用" : (forceStatus.equals("ADJUST") ? "建议微调" : "调整余量受限"));
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

    private String formatMillimeter(Number value) {
        if (value == null) {
            return "暂无推荐";
        }
        double d = value.doubleValue();
        if (Math.abs(d - Math.rint(d)) < 0.001) {
            return (int) Math.rint(d) + "mm";
        }
        return String.format(Locale.US, "%.1fmm", d);
    }
}
