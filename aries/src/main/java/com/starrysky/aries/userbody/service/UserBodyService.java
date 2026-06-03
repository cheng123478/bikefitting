package com.starrysky.aries.userbody.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBody;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.staticfitting.service.FittingConstants;
import com.starrysky.aries.staticfitting.service.StaticFittingCalculator;
import com.starrysky.aries.user.repository.UserRepository;
import com.starrysky.aries.user.service.UserService;
import com.starrysky.aries.userbody.repository.UserBodyRepository;
import com.starrysky.aries.userbody.schema.UserBodyComparisonVO;
import com.starrysky.aries.userbody.schema.UserBodyDTO;
import com.starrysky.aries.userbody.schema.UserBodyVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户身体信息业务服务类
 * 责身体档案的版本化管理及生物力学画像生成
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 15:42
 */
@Service
@Slf4j
public class UserBodyService {
    @Resource
    private UserBodyRepository userBodyRepository;
    
    @Resource
    private UserRepository userRepository;

    @Resource
    private UserService userService;

    /**
     * 保存新的身体档案
     * 采用增量保存策略，不覆盖旧记录，以便追踪用户的身体机能进化（如柔韧性提升）。
     *
     * @param dto 包含原始测量数据的 DTO
     * @return 包含分析结论的响应 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public UserBodyVO saveUserBody(UserBodyDTO dto, User user) {
        // 1. 深度生物力学校验
        validateBiometrics(dto);

        // 2. Fetch the latest managed user entity to update birthday/gender if necessary
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        boolean userUpdated = false;
        if (managedUser.getBirthday() == null && dto.getBirthday() != null) {
            managedUser.setBirthday(dto.getBirthday());
            userUpdated = true;
        }
        if (managedUser.getGender() == null && dto.getGender() != null) {
            managedUser.setGender(dto.getGender());
            userUpdated = true;
        }

        if (userUpdated) {
            userRepository.save(managedUser);
            userService.updateRedisCache(managedUser.getId());
            log.info("已静默更新用户生日或性别：User={}", managedUser.getId());
        }

        // 3. 手动映射Entity
        UserBody userBody = mapToEntity(dto);
        userBody.setUser(managedUser);
        UserBody saved = userBodyRepository.save(userBody);

        log.info("身体档案已保存：User={}, RecordID={}", dto.getUserId(), saved.getId());

        // 4. 构建画像报告
        return convertToVO(saved);
    }

    /**
     * 获取用户当前最新的身体档案及画像
     *
     * @param userId 用户标识
     * @return 最新档案 VO，若无记录则返回 null
     */
    public UserBodyVO getLatestUserBody(Long userId) {
        return userBodyRepository.findTopByUserIdOrderByIdDesc(userId)
                .map(this::convertToVO)
                .orElse(null);
    }

    /**
     * 获取用户身体发展历史
     *
     * @param userId 用户标识
     * @return 历史档案列表(按时间倒序)
     */
    public Page<UserBodyVO> getUserBodyHistory(Long userId, Pageable pageable) {
        Page<UserBody> page = userBodyRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        return page.map(this::convertToVO);
    }

    /**
     * 【核心算法引擎】
     * 将 Entity 转化为带分析结论的VO。
     * 包含：猿臂比计算、股胫比判定、柔韧性评分系统等。
     *
     * @param userBody 待转换的Entity
     * @return 转换后的VO
     */
    private UserBodyVO convertToVO(UserBody userBody) {
        UserBodyVO vo = new UserBodyVO();
        mapToVO(userBody, vo);
        // 1. 处理 User 基础信息 (年龄、性别)
        if (userBody.getUser() != null) {
            if (userBody.getUser().getBirthday() != null) {
                vo.setAge((int) ChronoUnit.YEARS.between(userBody.getUser().getBirthday(), LocalDate.now()));
            }
            if (userBody.getUser().getGender() != null) {
                vo.setGender(userBody.getUser().getGender());
            }
        }
        // --- 2. 差异化计算逻辑 (防御式编程) ---
        // A. 计算猿臂比 (需要：身高、肩宽、大臂、小臂)
        if (isAllPositive(userBody.getHeight(), userBody.getShoulderWidth(),
                userBody.getLongArmLength(), userBody.getShortArmLength())) {
            vo.setApeIndex(BiometricsCalculator.calculateApeIndex(
                    userBody.getHeight(), userBody.getShoulderWidth(),
                    userBody.getLongArmLength(), userBody.getShortArmLength()));
        }
        // B. 计算股胫比 (需要：大腿、小腿)
        if (isAllPositive(userBody.getThighLength(), userBody.getCalfLength())) {
            vo.setFemurTibiaRatio(BiometricsCalculator.calculateFemurRatio(
                    userBody.getThighLength(), userBody.getCalfLength()));
        }
        // C. 计算躯干占比 (需要：身高、胸高、跨高)
        if (isAllPositive(userBody.getHeight(), userBody.getBreastHeight(), userBody.getSpanHeight())) {
            vo.setTrunkRatio(BiometricsCalculator.calculateTrunkRatio(
                    userBody.getHeight(), userBody.getBreastHeight(), userBody.getSpanHeight()));
        }
        // D. 柔韧性得分 (需要：胸椎旋转、举臂角度。痛感默认为0不影响)
        // 注意：即使数据为0也算有效角度，但我们通常检查是否录入
        if (isAllPositive(userBody.getThoracicRotationAngle(), userBody.getArmRaiseAngle())) {
            vo.setFlexibilityScore(BiometricsCalculator.calculateFlexibilityScore(
                    userBody.getThoracicRotationAngle(),
                    userBody.getArmRaiseAngle(),
                    userBody.getThoracicPainLevel() != null ? userBody.getThoracicPainLevel() : 0));
        }
        // E. 核心稳定性得分 (深蹲60% + 平板支撑40%)
        Integer squatScore = userBody.getSquatStabilityScore();
        Integer plankScore = userBody.getPlankScore();
        if (squatScore != null && squatScore > 0) {
            double coreScore = squatScore * 0.6;
            if (plankScore != null && plankScore > 0) {
                coreScore += plankScore * 0.4;
            }
            vo.setCoreScore(coreScore);
            vo.setStabilityScore((int) Math.round(coreScore * 20));
        }
        // 3. 生成专家建议 (内部已做 null 判定)
        vo.setBmi(StaticFittingCalculator.calculateBmi(userBody.getHeight(), userBody.getWeightKg()));
        vo.setWaistToHeightRatio(StaticFittingCalculator.calculateWaistToHeightRatio(userBody.getHeight(), userBody.getWaistCircumference()));
        generateExpertPrescription(vo, userBody);
        // 4. 基础器材建议 (基于跨高、柔韧性、核心稳定性等综合计算)
        if (userBody.getSpanHeight() != null && userBody.getSpanHeight() > 0) {
            // 4.1 建议坐垫高度: saddleHeight = 跨高 * 0.883
            double baseSaddleHeight = userBody.getSpanHeight() * 0.883;
            // 如果柔韧性 < 50，坐高-3~5mm
            if (vo.getFlexibilityScore() != null && vo.getFlexibilityScore() < 50) {
                baseSaddleHeight -= 4; // 取中间值
            }
            vo.setRecSaddleHeight(String.format("%dmm", (int) Math.round(baseSaddleHeight)));

            // 4.2 建议坐垫类型
            StringBuilder saddleTypeAdvice = new StringBuilder();
            // 如果性别为女，输出短鼻头 / 中空
            if (vo.getGender() != null && vo.getGender() == 2) {
                saddleTypeAdvice.append("短鼻头/中空");
            }
            // 如果坐骨间距 > 130，输出宽坐垫
            if (userBody.getIschialDistance() != null && userBody.getIschialDistance() > 130) {
                if (saddleTypeAdvice.length() > 0) {
                    saddleTypeAdvice.append(" + ");
                }
                saddleTypeAdvice.append("宽坐垫");
            }
            if (saddleTypeAdvice.length() == 0) {
                saddleTypeAdvice.append("标准坐垫");
            }
            vo.setRecSaddleType(saddleTypeAdvice.toString());

            // 4.3 建议车把高度（Stack）: Stack = 坐垫高度 - Drop
            // baseDrop = 60~90 mm，根据柔韧性调整
            int baseDrop = 75; // 取中间值
            if (vo.getFlexibilityScore() != null) {
                if (vo.getFlexibilityScore() < 50) {
                    baseDrop = 50; // 40~60取中间值
                } else if (vo.getFlexibilityScore() < 80) {
                    baseDrop = 70; // 60~80取中间值
                } else {
                    baseDrop = 95; // 80~110取中间值
                }
            }
            // 如果疼痛等级 >= 3，drop -= 20
            if (userBody.getThoracicPainLevel() != null && userBody.getThoracicPainLevel() >= 3) {
                baseDrop -= 20;
            }
            // 如果核心稳定性 <= 2，drop -= 10
            if (vo.getCoreScore() != null && vo.getCoreScore() <= 2) {
                baseDrop -= 10;
            }
            // 如果年龄 > 45，drop -= 10
            if (vo.getAge() != null && vo.getAge() > 45) {
                baseDrop -= 10;
            }
            // 确保drop不低于20
            if (baseDrop < 20) baseDrop = 20;
            int stack = (int) Math.round(baseSaddleHeight) - baseDrop;
            vo.setRecStack(String.format("%dmm", stack));

            // 4.4 建议前伸距离（Reach）: reach = （大臂长+小臂长） * 0.45
            if (isAllPositive(userBody.getLongArmLength(), userBody.getShortArmLength())) {
                double baseReach = (userBody.getLongArmLength() + userBody.getShortArmLength()) * 0.45;
                // 如果猿臂比 > 1.04，reach += 10~20
                if (vo.getApeIndex() != null && vo.getApeIndex() > 1.04) {
                    baseReach += 15; // 取中间值
                }
                // 如果猿臂比 < 0.98，reach -= 10~20
                if (vo.getApeIndex() != null && vo.getApeIndex() < 0.98) {
                    baseReach -= 15; // 取中间值
                }
                // 如果柔韧性 < 50，reach -= 10
                if (vo.getFlexibilityScore() != null && vo.getFlexibilityScore() < 50) {
                    baseReach -= 10;
                }
                // 如果举臂角度 < 150，reach -= 10~15
                if (userBody.getArmRaiseAngle() != null && userBody.getArmRaiseAngle() < 150) {
                    baseReach -= 12.5; // 取中间值
                }
                // 如果疼痛等级 >= 3，reach -= 10
                if (userBody.getThoracicPainLevel() != null && userBody.getThoracicPainLevel() >= 3) {
                    baseReach -= 10;
                }
                vo.setRecReach(String.format("%dmm", (int) Math.round(baseReach)));
            } else {
                vo.setRecReach("待测量手臂数据");
            }
        }
        return vo;
    }

    /**
     * 辅助方法：检查一组数值是否非空且大于0
     */
    private boolean isAllPositive(Number... numbers) {
        for (Number n : numbers) {
            if (n == null || n.doubleValue() <= 0) return false;
        }
        return true;
    }

    /**
     * 生物力学常模合法性校验
     * 目的：在数据入库前拦截由于测量失误（如单位填错、量错位置）导致的异常数据。
     *
     * @param dto 包含原始测量数据的 DTO
     */
    private void validateBiometrics(UserBodyDTO dto) {
        // --- 1. 核心必填项 ---
        // 身高是必填项，跨高可选（用于智能推荐场景）
        if (dto.getHeight() == null) {
            throw new BizException(500, "基础身体数据缺失：[身高] 是必需的。");
        }

        // --- 2. 基础逻辑与单位校验 ---
        validateIntegerRange("身高", dto.getHeight(), 1200, 2200, "mm");
        validateDoubleRange("体重", dto.getWeightKg(), 30.0, 250.0, "kg");
        validateIntegerRange("跨高", dto.getSpanHeight(), 600, 1100, "mm");
        validateIntegerRange("腰围", dto.getWaistCircumference(), 400, 1800, "mm");
        validateIntegerRange("胸高", dto.getBreastHeight(), 900, 1800, "mm");
        validateIntegerRange("肩宽", dto.getShoulderWidth(), 200, 600, "mm");
        validateIntegerRange("坐骨间距", dto.getIschialDistance(), 50, 200, "mm");
        validateIntegerRange("大臂长", dto.getLongArmLength(), 180, 500, "mm");
        validateIntegerRange("小臂长", dto.getShortArmLength(), 160, 450, "mm");
        validateIntegerRange("大腿长", dto.getThighLength(), 250, 650, "mm");
        validateIntegerRange("小腿长", dto.getCalfLength(), 250, 650, "mm");
        validateIntegerRange("脚长", dto.getFootLength(), 180, 350, "mm");

        if (dto.getLongArmLength() != null && dto.getShortArmLength() != null) {
            double armRatio = (double) dto.getLongArmLength() / dto.getShortArmLength();
            if (armRatio < 0.7 || armRatio > 1.5) {
                throw new BizException(500, "比例异常：[大臂长] 与 [小臂长] 比例异常，请重新测量。");
            }
        }
        if (dto.getThighLength() != null && dto.getCalfLength() != null) {
            double legRatio = (double) dto.getThighLength() / dto.getCalfLength();
            if (legRatio < 0.7 || legRatio > 1.5) {
                throw new BizException(500, "比例异常：[大腿长] 与 [小腿长] 比例异常，请重新测量。");
            }
        }
        
        // 只有当跨高存在时才进行跨高相关校验
        if (dto.getSpanHeight() != null) {
            if (dto.getHeight() <= dto.getSpanHeight()) {
                throw new BizException(500, "逻辑错误：[身高] 必须大于 [跨高]。");
            }

            // --- 3. 肢体比例画像校验（防呆逻辑） ---
            // 跨高占比校验 (正常范围 0.35 - 0.6)
            double inseamRatio = (double) dto.getSpanHeight() / dto.getHeight();
            if (inseamRatio < 0.35 || inseamRatio > 0.6) {
                throw new BizException(500, "比例异常：[跨高] 与 [身高] 比例不符合常人，请重新测量。");
            }
        }

        // 躯干长度校验
        if (dto.getBreastHeight() != null && dto.getSpanHeight() != null) {
            if (dto.getBreastHeight() <= dto.getSpanHeight()) {
                throw new BizException(500, "逻辑错误：[胸高] 必须大于 [跨高]，否则无法计算躯干长度。");
            }
        }

        // --- 4. 灵活性数据范围校验 (防止异常输入) ---
        if (dto.getArmRaiseAngle() != null && (dto.getArmRaiseAngle() < 0 || dto.getArmRaiseAngle() > 180)) {
            throw new BizException(500, "范围错误：[举臂角度] 必须在 0-180 度之间。");
        }
        if (dto.getThoracicRotationAngle() != null) {
            Double angle = dto.getThoracicRotationAngle();
            if (angle != 30 && angle != 50 && angle != 70 && angle != 85) {
                throw new BizException(500, "选项错误：[坐姿抱胸旋转] 必须选择提供的选项之一（30°/50°/70°/85°）。");
            }
        }

        // 评分区间校验
        if (dto.getThoracicPainLevel() != null && (dto.getThoracicPainLevel() < 0 || dto.getThoracicPainLevel() > 5)) {
            throw new BizException(500, "范围错误：[痛感等级] 必须在 0-5 之间。");
        }
        if (dto.getSquatStabilityScore() != null && (dto.getSquatStabilityScore() < 1 || dto.getSquatStabilityScore() > 5)) {
            throw new BizException(500, "范围错误：[深蹲稳定性评分] 必须在 1-5 之间。");
        }
        if (dto.getPlankScore() != null && (dto.getPlankScore() < 1 || dto.getPlankScore() > 5)) {
            throw new BizException(500, "范围错误：[平板支撑评分] 必须在 1-5 之间。");
        }

        log.debug("用户 {} 的生物力学数据通过高阶常模校验。", dto.getUserId());
    }

    private void validateIntegerRange(String label, Integer value, int min, int max, String unit) {
        if (value != null && (value < min || value > max)) {
            throw new BizException(500, String.format("数值异常：[%s] 请确认单位为 %s，正常范围 %d-%d%s。", label, unit, min, max, unit));
        }
    }

    private void validateDoubleRange(String label, Double value, double min, double max, String unit) {
        if (value != null && (value < min || value > max)) {
            throw new BizException(500, String.format("数值异常：[%s] 请确认单位为 %s，正常范围 %.0f-%.0f%s。", label, unit, min, max, unit));
        }
    }

    private void generateExpertPrescription(UserBodyVO vo, UserBody entity) {
        List<String> tags = new ArrayList<>();
        List<String> findings = new ArrayList<>();
        List<String> advice = new ArrayList<>();
        List<String> priorities = new ArrayList<>();

        // BMI 与腰高比只作为筛查和空间判断线索，结论必须回到骑行姿态耐受，而不是健康诊断。
        if (vo.getGender() != null) {
            if (vo.getGender() == 2) {
                addUnique(tags, "女性骑行者");
            } else if (vo.getGender() == 1) {
                addUnique(tags, "男性骑行者");
            }
        }

        if (vo.getAge() != null) {
            if (vo.getAge() < 25) {
                addUnique(tags, "高适应潜力");
            } else if (vo.getAge() > 45) {
                addUnique(tags, "稳定优先");
                addUnique(priorities, "恢复与活动度维护");
                addUnique(advice, "恢复与活动度维护：训练日之间保留恢复窗口；每周安排 2-3 次低强度力量或活动度练习，重点照顾髋、胸椎、肩颈和腰背。");
            }
        }

        boolean hasAbdominalNeed = StaticFittingCalculator.hasAbdominalClearanceNeed(vo.getBmi(), vo.getWaistToHeightRatio());
        boolean hasSevereAbdominalNeed = StaticFittingCalculator.isSevereAbdominalClearanceNeed(vo.getWaistToHeightRatio());
        if (hasSevereAbdominalNeed) {
            addUnique(tags, "腹部空间高优先级");
            addUnique(priorities, "体重与腰围管理");
            addUnique(findings, String.format("体重与腰围方面：你的腰高比约为 %.2f，提示腹部空间需要优先关注；这不等于健康诊断，但会影响前倾时的呼吸和髋腹活动余量。", vo.getWaistToHeightRatio()));
            addUnique(advice, buildWeightManagementAdvice());
        } else if (hasAbdominalNeed) {
            addUnique(tags, "腹部空间需预留");
            addUnique(priorities, "体重与腰围管理");
            addUnique(findings, String.format("体重与腰围方面：你的 BMI 约为 %.1f，腰高比约为 %.2f，两项一起提示腹部空间和呼吸舒适度需要关注。", vo.getBmi(), vo.getWaistToHeightRatio()));
            addUnique(advice, buildWeightManagementAdvice());
        } else if (vo.getBmi() != null && vo.getWaistToHeightRatio() == null
                && vo.getBmi() >= FittingConstants.BMI_OBESITY_THRESHOLD) {
            addUnique(tags, "腹部空间待确认");
            addUnique(priorities, "体重与腰围管理");
            addUnique(findings, String.format("体重与腰围方面：你的 BMI 约为 %.1f，但 BMI 不能区分肌肉量和脂肪分布；缺少腰围时，只能先把腹部空间列为待确认。", vo.getBmi()));
            addUnique(advice, buildWeightManagementAdvice());
            addUnique(advice, "记录建议：补录腰围，并每 2-4 周固定同一时间记录体重和腰围趋势，比单次体重更有参考价值。");
        } else if (vo.getBmi() != null && vo.getBmi() >= 25.0 && vo.getWaistToHeightRatio() == null) {
            addUnique(tags, "腰围待补录");
            addUnique(priorities, "体重与腰围管理");
            addUnique(findings, String.format("体重与腰围方面：你的 BMI 约为 %.1f，建议补录腰围辅助判断腹部空间；BMI 本身不用于评价骑行能力。", vo.getBmi()));
            addUnique(advice, "轻量体重管理建议：减少含糖饮料和高频夜宵，维持规律骑行或快走；先追求稳定习惯，不追求短期大幅下降。");
        }

        if (vo.getApeIndex() != null && vo.getApeIndex() > 1.04) {
            addUnique(tags, "长臂优势");
        }
        if (vo.getApeIndex() != null && vo.getApeIndex() < 0.98) {
            addUnique(priorities, "肩颈与肩胛控制");
            addUnique(findings, String.format("上肢前伸方面：你的猿臂比约为 %.2f，上肢比例紧凑；系统已为你推荐更集中的座舱坐标，让肩膀自然承重、减少前倾压力。", vo.getApeIndex()));
            addUnique(advice, "肩颈优化建议：做肩胛后缩下压、弹力带划船、墙天使和胸小肌放松，每周 2-3 次；目标是让肩膀放松承重，发挥紧凑比例的优势。");
        }

        if (vo.getFemurTibiaRatio() != null && vo.getFemurTibiaRatio() > 1.15) {
            addUnique(tags, "长股骨特征");
            addUnique(priorities, "髋膝控制");
            addUnique(findings, String.format("下肢发力方面：你的大腿/小腿比例约为 %.2f，大腿相对更长；发力时更需要稳定髋关节和膝盖轨迹。", vo.getFemurTibiaRatio()));
            addUnique(advice, "髋膝控制建议：加入臀桥、分腿蹲、台阶下蹲和蚌式开合，每周 2-3 次；动作中注意膝盖对准脚尖，避免内扣。");
        }
        if (vo.getFemurTibiaRatio() != null && vo.getFemurTibiaRatio() < 0.9) {
            addUnique(tags, "长小腿特征");
            addUnique(priorities, "踝关节控制");
            addUnique(findings, String.format("下肢发力方面：你的大腿/小腿比例约为 %.2f，小腿相对更长；踩踏中更容易用踝关节补偿。", vo.getFemurTibiaRatio()));
            addUnique(advice, "踝关节控制建议：做提踵离心下降、胫骨前肌抬脚尖和小腿后侧拉伸，每周 2-3 次；目标是减少踩踏底部过度垫脚尖。");
        }

        boolean lowThoracicRotation = entity.getThoracicRotationAngle() != null && entity.getThoracicRotationAngle() < 40;
        boolean lowShoulderFlexion = entity.getArmRaiseAngle() != null && entity.getArmRaiseAngle() < 150;
        boolean hasPainRisk = entity.getThoracicPainLevel() != null && entity.getThoracicPainLevel() >= 3;
        if (vo.getFlexibilityScore() != null) {
            if (vo.getFlexibilityScore() > 80) {
                addUnique(tags, "柔韧性优秀");
            }
        }
        if (lowThoracicRotation || lowShoulderFlexion || (vo.getFlexibilityScore() != null && vo.getFlexibilityScore() < 50)) {
            addUnique(priorities, "柔韧性与活动度");
            addUnique(findings, buildUpperBodyFinding(vo, entity, lowThoracicRotation, lowShoulderFlexion));
            addUnique(advice, "柔韧性提升建议：重点拉伸腘绳肌、髋屈肌、臀肌和胸小肌，每个动作保持 20-30 秒、2-3 组，每周 3-5 次；循序渐进，享受过程。");
            addUnique(advice, "胸椎与肩部活动建议：开书式、四点跪姿胸椎旋转、泡沫轴胸椎伸展、墙天使，每周 2-3 次；目标是前倾时肩颈放松、呼吸顺畅。");
        }
        if (hasPainRisk) {
            addUnique(priorities, "先处理疼痛信号");
            addUnique(findings, "疼痛与恢复方面：你在动作测试中出现中高等级不适，系统已为你调整至更温和的座舱坐标，优先保障恢复与舒适。");
            addUnique(advice, "恢复建议：近期先降低训练刺激，避免硬拉伸和强行压低姿态；如果疼痛持续、放射或影响日常活动，建议找运动医学或康复专业人士评估。");
        }

        boolean lowSquat = entity.getSquatStabilityScore() != null && entity.getSquatStabilityScore() <= 2;
        boolean lowPlank = entity.getPlankScore() != null && entity.getPlankScore() <= 2;
        if (lowSquat || lowPlank || (vo.getStabilityScore() != null && vo.getStabilityScore() < 50)) {
            addUnique(priorities, "长时间姿态稳定");
            addUnique(findings, buildCoreFinding(vo, entity));
            addUnique(advice, "核心抗旋转与骨盆控制：死虫、鸟狗、侧桥，每周 2-3 次。");
            addUnique(advice, "核心耐力培养：平板支撑、侧桥、臀桥，从短组多次开始；先保证腰椎不塌、骨盆稳定，再逐步延长时间。");
        }

        if (vo.getApeIndex() == null) {
            addUnique(advice, "补录肩宽、大臂长和小臂长后，可进一步判断 Reach 是否需要偏短或偏长。");
        }
        if (vo.getFemurTibiaRatio() == null) {
            addUnique(advice, "补录大腿长和小腿长后，可进一步复核座垫前后位置与锁片设定。");
        }

        String summary = buildUserCenteredSummary(findings, priorities);

        vo.setRiderTags(tags);
        vo.setSummary(summary);
        vo.setFindings(findings);
        vo.setAdvice(advice);
    }

    private String buildWeightManagementAdvice() {
        return "体重与腰围建议：从可持续的小改动开始，减少含糖饮料和高频夜宵，保持规律骑行、快走或其他低冲击有氧；每周加入 2 次基础力量训练，先看腰围和精神状态的趋势。";
    }

    private String buildUpperBodyFinding(UserBodyVO vo, UserBody entity, boolean lowThoracicRotation, boolean lowShoulderFlexion) {
        List<String> signals = new ArrayList<>();
        if (vo.getFlexibilityScore() != null) {
            signals.add("柔韧性评分 " + vo.getFlexibilityScore() + " 分");
        }
        if (lowThoracicRotation) {
            signals.add("胸椎旋转 " + entity.getThoracicRotationAngle() + "°");
        }
        if (lowShoulderFlexion) {
            signals.add("举臂角度 " + entity.getArmRaiseAngle() + "°");
        }
        String signalText = signals.isEmpty() ? "上身活动度数据" : String.join("、", signals);
        return "柔韧性与活动度方面：你的" + signalText + "提示系统已为你推荐更舒适的座舱坐标；通过规律拉伸，肩颈和腰背会更加放松。";
    }

    private String buildCoreFinding(UserBodyVO vo, UserBody entity) {
        List<String> signals = new ArrayList<>();
        if (vo.getStabilityScore() != null) {
            signals.add("稳定性评分 " + vo.getStabilityScore() + " 分");
        }
        if (entity.getSquatStabilityScore() != null && entity.getSquatStabilityScore() <= 2) {
            signals.add("深蹲稳定性偏低");
        }
        if (entity.getPlankScore() != null && entity.getPlankScore() <= 2) {
            signals.add("平板支撑耐力偏低");
        }
        String signalText = signals.isEmpty() ? "核心稳定性数据" : String.join("、", signals);
        return "核心稳定性方面：你的" + signalText + "提示通过规律训练，核心对长时间骑行的支撑会逐步增强；系统已为你推荐更友好的座舱坐标。";
    }

    private String buildUserCenteredSummary(List<String> findings, List<String> priorities) {
        if (findings.isEmpty()) {
            return "从你已录入的数据看，目前没有明显高优先级的身体限制信号。后续建议以可持续输出为基准，结合真实骑行中的手麻、肩颈、腰背、膝盖和呼吸反馈做小幅微调。";
        }
        if (priorities.isEmpty()) {
            return "从你的数据看，本次需要关注的不是单一尺寸，而是多个身体信号叠加后的姿态耐受。建议先看下方方面，再按补弱建议逐项改善。";
        }
        int limit = Math.min(3, priorities.size());
        String priorityText = String.join("、", priorities.subList(0, limit));
        return "从你的数据看，当前优先关注方面是：" + priorityText + "。先把身体本身的活动度、稳定性和恢复能力补起来，再谈更激进的车上姿态，会更稳也更可持续。";
    }

    private void addUnique(List<String> values, String value) {
        if (value == null || value.isBlank() || values.contains(value)) {
            return;
        }
        values.add(value);
    }

    private UserBody mapToEntity(UserBodyDTO d) {
        UserBody e = new UserBody();
        e.setHeight(d.getHeight());
        e.setSpanHeight(d.getSpanHeight());
        e.setBreastHeight(d.getBreastHeight());
        e.setShoulderWidth(d.getShoulderWidth());
        e.setLongArmLength(d.getLongArmLength());
        e.setShortArmLength(d.getShortArmLength());
        e.setThighLength(d.getThighLength());
        e.setCalfLength(d.getCalfLength());
        e.setIschialDistance(d.getIschialDistance());
        e.setArmRaiseAngle(d.getArmRaiseAngle());
        e.setThoracicRotationAngle(d.getThoracicRotationAngle());
        e.setThoracicPainLevel(d.getThoracicPainLevel());
        e.setSquatDepth(d.getSquatDepth());
        e.setSquatStabilityScore(d.getSquatStabilityScore());
        e.setPlankScore(d.getPlankScore());
        e.setFootLength(d.getFootLength());
        e.setWeightKg(d.getWeightKg());
        e.setWaistCircumference(d.getWaistCircumference());
        return e;
    }

    private void mapToVO(UserBody e, UserBodyVO v) {
        v.setId(e.getId());
        v.setCreateTime(e.getCreateTime());
        v.setHeight(e.getHeight());
        v.setSpanHeight(e.getSpanHeight());
        v.setBreastHeight(e.getBreastHeight());
        v.setShoulderWidth(e.getShoulderWidth());
        v.setLongArmLength(e.getLongArmLength());
        v.setShortArmLength(e.getShortArmLength());
        v.setThighLength(e.getThighLength());
        v.setCalfLength(e.getCalfLength());
        v.setIschialDistance(e.getIschialDistance());
        v.setFootLength(e.getFootLength());
        v.setWeightKg(e.getWeightKg());
        v.setWaistCircumference(e.getWaistCircumference());
        v.setArmRaiseAngle(e.getArmRaiseAngle());
        v.setThoracicRotationAngle(e.getThoracicRotationAngle());
        v.setThoracicPainLevel(e.getThoracicPainLevel());
        v.setSquatDepth(e.getSquatDepth());
        v.setSquatStabilityScore(e.getSquatStabilityScore());
        v.setPlankScore(e.getPlankScore());
    }

    /**
     * 获取用户身体档案比较结果
     *
     * @param id1 待比较的记录ID
     * @param id2 待比较的记录ID
     * @return 身体档案比较结果
     */
    public UserBodyComparisonVO compareRecords(Long id1, Long id2) {
        // 1. 获取两条记录，并根据时间自动排序（确保老的在前，新的在后）
        UserBody r1 = userBodyRepository.findById(id1)
                .orElseThrow(() -> new BizException(500, "记录ID " + id1 + " 不存在"));
        UserBody r2 = userBodyRepository.findById(id2)
                .orElseThrow(() -> new BizException(500, "记录ID " + id2 + " 不存在"));

        UserBody base = r1.getCreateTime().isBefore(r2.getCreateTime()) ? r1 : r2;
        UserBody target = r1.getCreateTime().isBefore(r2.getCreateTime()) ? r2 : r1;

        // 2. 转换为 VO
        UserBodyVO baseVO = convertToVO(base);
        UserBodyVO targetVO = convertToVO(target);

        // 3. 构建差异矩阵 (Matrix)
        List<UserBodyComparisonVO.MetricDiff> matrix = new ArrayList<>();

        // 维度 A: 综合机能评分
        addMetricToMatrix(matrix, "柔韧性评分", "分",
                baseVO.getFlexibilityScore(), targetVO.getFlexibilityScore(),
                "决定了身体对激进气动姿态的耐受上限");

        addMetricToMatrix(matrix, "核心稳定性", "%",
                baseVO.getStabilityScore(), targetVO.getStabilityScore(),
                "得分提升可显著减少高强度骑行时的骨盆晃动");

        // 维度 B: 核心关节角度
        addMetricToMatrix(matrix, "胸椎旋转", "°",
                baseVO.getThoracicRotationAngle(), targetVO.getThoracicRotationAngle(),
                "度数增加意味着在下把位时胸腔压缩减少，摄氧量更佳");

        addMetricToMatrix(matrix, "举臂角度", "°",
                baseVO.getArmRaiseAngle(), targetVO.getArmRaiseAngle(),
                "反映肩膀灵活性，影响抬头看路时的颈椎代偿压力");

        // 维度 C: 核心稳定性
        addMetricToMatrix(matrix, "平板支撑", "级",
                baseVO.getPlankScore(), targetVO.getPlankScore(),
                "核心耐力提升有助于减少长途骑行时的躯干晃动");

        // 4. 组装结果
        UserBodyComparisonVO comparison = new UserBodyComparisonVO();
        comparison.setBaseRecord(baseVO);
        comparison.setTargetRecord(targetVO);
        comparison.setDiffMatrix(matrix);

        // 5. 生成专家点评
        comparison.setEvolutionSummary(generateEvolutionSummary(comparison));

        return comparison;
    }

    /**
     * 将指标添加到差异矩阵中
     *
     * @param matrix       差异矩阵
     * @param label        指标标签
     * @param unit         指标单位
     * @param v1           基准值
     * @param v2           目标值
     * @param significance 指标意义
     */
    private void addMetricToMatrix(List<UserBodyComparisonVO.MetricDiff> matrix,
                                   String label, String unit,
                                   Number v1, Number v2, String significance) {
        UserBodyComparisonVO.MetricDiff diff = new UserBodyComparisonVO.MetricDiff();
        diff.setLabel(label);
        diff.setUnit(unit);
        diff.setBaseVal(v1);
        diff.setTargetVal(v2);
        diff.setSignificance(significance);
        // 核心判定逻辑：处理空值
        if (v1 == null || v2 == null) {
            diff.setDelta(0.0);
            if (v1 == null && v2 != null) {
                // 原本没填，现在填了
                diff.setTrend("NEW_DATA");
            } else if (v1 != null && v2 == null) {
                // 原本填了，现在漏填了
                diff.setTrend("MISSING");
            } else {
                // 两者都空
                diff.setTrend("VOID");
            }
        } else {
            // 两者都有数，执行正常计算
            double val1 = Double.parseDouble(v1.toString());
            double val2 = Double.parseDouble(v2.toString());
            double delta = val2 - val1;
            diff.setDelta(delta);
            diff.setTrend(delta > 0 ? "UP" : (delta < 0 ? "DOWN" : "STABLE"));
        }

        matrix.add(diff);
    }

    /**
     * 核心逻辑：基于变化产生专家点评文案
     *
     * @param comp 比较结果对象
     * @return 点评文案
     */
    private String generateEvolutionSummary(UserBodyComparisonVO comp) {
        UserBodyVO base = comp.getBaseRecord();
        UserBodyVO target = comp.getTargetRecord();
        // 防御：如果没有柔韧性得分，则无法生成针对进化的评价
        if (base.getFlexibilityScore() == null || target.getFlexibilityScore() == null) {
            return "Fittor 提示：由于历史数据或当前数据中的机能测试不完整，暂无法生成深度进化评估。建议完整录入\"身体图谱\"中的三项柔韧性测试。";
        }
        int scoreDiff = comp.getTargetRecord().getFlexibilityScore() - comp.getBaseRecord().getFlexibilityScore();
        StringBuilder sb = new StringBuilder("Fittor 进化诊断：");
        if (scoreDiff > 10) {
            sb.append("您的身体机能正处于显著上升期！特别是柔韧性的跨越式提升，为您解锁了更具攻击性的 Fitting 设定空间。建议尝试降低 5mm 把立落差。");
        } else if (scoreDiff > 0) {
            sb.append("身体状态保持良好并有小幅精进，持续的拉伸和核心训练正在生效，这有助于提升长途骑行的舒适度稳定性。");
        } else if (scoreDiff < 0) {
            sb.append("监测到近期机能评分有所回落。如果您最近感到身体僵硬，建议增加针对性的胸椎活动度训练，避免在车上出现代偿动作。");
        } else {
            sb.append("身体各项参数处于稳定期。目前的 Fitting 设定依然匹配您的当前状态。");
        }

        return sb.toString();
    }

}
