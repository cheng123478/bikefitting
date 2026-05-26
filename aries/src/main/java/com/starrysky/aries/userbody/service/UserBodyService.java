package com.starrysky.aries.userbody.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBody;
import com.starrysky.aries.exception.BizException;
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
        if (dto.getHeight() < 500) {
            throw new BizException(500, "数值异常：身高数据过小，请确保单位为[毫米(mm)]。");
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
        if (dto.getBreastHeight() != null) {
            if (dto.getBreastHeight() <= dto.getSpanHeight()) {
                throw new BizException(500, "逻辑错误：[胸高] 必须大于 [跨高]，否则无法计算躯干长度。");
            }
        }

        // 肩宽与坐骨校验
        if (dto.getShoulderWidth() != null && (dto.getShoulderWidth() < 200 || dto.getShoulderWidth() > 600)) {
            throw new BizException(500, "数值异常：[肩宽] 填写错误（建议范围 200-600mm）。");
        }
        if (dto.getIschialDistance() != null && (dto.getIschialDistance() < 50 || dto.getIschialDistance() > 200)) {
            throw new BizException(500, "数值异常：[坐骨间距] 填写错误（建议范围 50-200mm）。");
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

    private void generateExpertPrescription(UserBodyVO vo, UserBody entity) {
        List<String> tags = new ArrayList<>();
        List<String> findings = new ArrayList<>();
        List<String> advice = new ArrayList<>();

        // 1. 性别特征分析
        if (vo.getGender() != null) {
            if (vo.getGender() == 2) {
                tags.add("女性骑行者");
                findings.add("骨盆结构相对更宽，耻骨角度更大，骑行时骨盆前倾角度更明显");
                advice.add("建议优先选择女性专用坐垫（短鼻头或中空设计），以减少会阴压迫，同时适当关注坐垫宽度匹配");
            } else if (vo.getGender() == 1) {
                tags.add("男性骑行者");
            }
        }

        // 2. 年龄段分析
        if (vo.getAge() != null) {
            if (vo.getAge() < 25) {
                tags.add("高适应潜力");
                findings.add("肌肉与关节适应能力强，对骑行姿态变化的耐受度较高");
                advice.add("在保证舒适性的前提下，可逐步尝试更低的把位和更大的落差，以提升气动表现");
            } else if (vo.getAge() > 45) {
                tags.add("稳定优先");
                findings.add("随着年龄增长，关节活动度与恢复能力下降，脊柱与腰背部代偿风险增加");
                advice.add("建议适当提高车头高度，减少过度前倾姿态，同时加强核心稳定性训练以保护腰背");
            }
        }

        // 3. 猿臂比分析
        if (vo.getApeIndex() != null && vo.getApeIndex() > 1.04) {
            tags.add("长臂优势");
            findings.add("上肢长度相对较长，具备更好的前伸能力与气动姿态适应性");
            advice.add("可以适当降低把位或增加前伸距离（Reach），在保证舒适的前提下优化空气动力学姿态");
        }
        if (vo.getApeIndex() != null && vo.getApeIndex() < 0.98) {
            tags.add("前伸受限");
            findings.add("上肢相对较短，前伸能力有限，过长的骑行姿态容易导致肩颈紧张");
            advice.add("建议缩短把立长度或提高车头高度，避免过度前伸带来的上肢疲劳");
        }

        // 4. 股胫比分析
        if (vo.getFemurTibiaRatio() != null && vo.getFemurTibiaRatio() > 1.15) {
            tags.add("长股骨特征");
            findings.add("大腿长度占比高，下蹲与骑行时骨盆更容易后移，重心偏后");
            advice.add("需要重点关注坐垫后移量（Setback）与高度匹配，避免踩踏时出现后坐或发力不顺");
        }
        if (vo.getFemurTibiaRatio() != null && vo.getFemurTibiaRatio() < 0.9) {
            tags.add("长小腿特征");
            findings.add("小腿比例较高，踝关节参与踩踏的幅度更大");
            advice.add("建议关注锁鞋位置与踩踏角度，避免踝关节过度参与导致疲劳");
        }

        // 5. 柔韧性分析
        if (vo.getFlexibilityScore() != null) {
            if (vo.getFlexibilityScore() < 50) {
                tags.add("柔韧性受限");
                findings.add("髋部与后链肌群（如腘绳肌）活动受限，身体前倾能力不足");
                advice.add("建议采用更保守的骑行姿态（较高车头），并优先进行后侧链拉伸训练");
            } else if (vo.getFlexibilityScore() > 80) {
                tags.add("柔韧性优秀");
                findings.add("关节活动范围大，具备良好的前倾能力与姿态适应能力");
                advice.add("可以尝试更低的把位与更大的落差，以提升骑行效率与气动表现");
            }
        }

        // 6. 疼痛风险分析
        if (entity.getThoracicPainLevel() != null && entity.getThoracicPainLevel() >= 3) {
            tags.add("疼痛风险");
            findings.add("在动作测试中出现明显不适或疼痛，提示当前身体状态可能存在功能性限制或潜在损伤风险");
            advice.add("建议降低骑行姿态激进程度，避免大幅度前倾，同时优先进行恢复与评估训练");
        }

        // 7. 核心稳定性分析
        if (entity.getSquatStabilityScore() != null) {
            if (entity.getSquatStabilityScore() <= 2) {
                tags.add("稳定性不足");
                findings.add("下肢与核心控制能力较弱，下蹲过程中存在明显晃动或代偿");
                advice.add("骑行中可能出现骨盆晃动，建议加强核心训练（如平板支撑、死虫训练）以提升稳定性");
            }
        }
        if (entity.getPlankScore() != null && entity.getPlankScore() <= 2) {
            tags.add("核心耐力不足");
            findings.add("核心肌群耐力较弱，长时间骑行中难以维持稳定姿态");
            advice.add("建议增加核心耐力训练（如平板支撑、侧桥），提升长时间骑行稳定性");
        }

        // 8. 肩部活动度分析
        if (entity.getArmRaiseAngle() != null && entity.getArmRaiseAngle() < 150) {
            tags.add("肩部活动受限");
            findings.add("肩关节活动范围不足，上肢前伸时容易产生紧张或代偿");
            advice.add("建议提高车头高度或缩短前伸距离，同时增加肩部活动度训练");
        }

        // 9. 躯干灵活性分析
        if (entity.getThoracicRotationAngle() != null && entity.getThoracicRotationAngle() < 40) {
            tags.add("躯干灵活性不足");
            findings.add("胸椎旋转能力不足，影响骑行中的身体协调与姿态调整");
            advice.add("建议增加躯干旋转训练，以提升骑行中的灵活性与舒适性");
        }

        // 10. 生成总结文案（基于已生成的tags）
        String summary = "根据您的身体测量与功能评估结果，";
        if (tags.contains("柔韧性受限")) {
            summary += "当前柔韧性存在一定限制，";
        }
        if (tags.contains("核心稳定性不足") || tags.contains("核心耐力不足")) {
            summary += "核心稳定性有待提升，";
        }
        if (tags.contains("疼痛风险")) {
            summary += "并伴随一定的疼痛风险，";
        }
        summary += "建议在保证舒适与安全的前提下进行骑行姿态调整，并结合针对性训练逐步改善身体能力。";

        // 11. 设置到VO中
        vo.setRiderTags(tags);
        vo.setSummary(summary);
        vo.setFindings(findings);
        vo.setAdvice(advice);
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
