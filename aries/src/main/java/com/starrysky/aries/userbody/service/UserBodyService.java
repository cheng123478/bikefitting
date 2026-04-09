package com.starrysky.aries.userbody.service;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBody;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.user.repository.UserRepository;
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
        // 4. 基础器材初判 (仅依赖跨高，Level 1 必填项)
        if (userBody.getSpanHeight() != null && userBody.getSpanHeight() > 0) {
            vo.setRecCrankLength((int) (Math.round(userBody.getSpanHeight() * 0.204 / 2.5) * 2.5));

            // 坐骨间距如果没有，则不提供具体宽度范围建议
            if (userBody.getIschialDistance() != null && userBody.getIschialDistance() > 0) {
                vo.setSaddleWidthAdvice((userBody.getIschialDistance() + 20) + " - " + (userBody.getIschialDistance() + 30) + "mm");
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
        if (dto.getHeight() == null || dto.getSpanHeight() == null) {
            throw new BizException(500, "基础身体数据缺失：[身高] 和 [跨高] 是 Fitting 的核心基准。");
        }

        // --- 2. 基础逻辑与单位校验 ---
        if (dto.getHeight() < 500) {
            throw new BizException(500, "数值异常：身高数据过小，请确保单位为[毫米(mm)]。");
        }
        if (dto.getHeight() <= dto.getSpanHeight()) {
            throw new BizException(500, "逻辑错误：[身高] 必须大于 [跨高]。");
        }

        // --- 3. 肢体比例画像校验（防呆逻辑） ---
        // 跨高占比校验 (正常范围 0.35 - 0.6)
        double inseamRatio = (double) dto.getSpanHeight() / dto.getHeight();
        if (inseamRatio < 0.35 || inseamRatio > 0.6) {
            throw new BizException(500, "比例异常：[跨高] 与 [身高] 比例不符合常人，请重新测量。");
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
        if (dto.getThoracicRotationAngle() != null && (dto.getThoracicRotationAngle() < 0 || dto.getThoracicRotationAngle() > 95)) {
            throw new BizException(500, "范围错误：[胸椎旋转角度] 必须在 0-95 度之间。");
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
        StringBuilder comment = new StringBuilder("Fittor 评估结论：");
        if (vo.getGender() != null) {
            if (vo.getGender() == 2) {
                tags.add("女性骑行者");
                comment.append("【女性专项】考虑到女性盆骨生理结构较宽且骑行时盆骨前倾角较大，建议优先关注女性专用短鼻头座垫，以减轻会阴部压迫。");
            } else if (vo.getGender() == 1) {
                tags.add("男性骑行者");
            }
        }
        if (vo.getAge() != null && vo.getAge() > 0) {
            if (vo.getAge() < 25) {
                tags.add("潜力骑士");
                comment.append("【年龄优势】您正处于身体机能巅峰，骨骼与肌肉的适应性极强，在后续 Fitting 中可尝试更具攻击性的气动设定。");
            } else if (vo.getAge() > 45) {
                tags.add("资深骑士");
                comment.append("【预防保护】随年龄增长，脊柱代偿风险增加。建议在后续调教中适当抬高 Stack(车头高度)，重点保护颈椎与下背部。");
            }
        }
        if (vo.getApeIndex() != null && vo.getApeIndex() > 1.04) {
            tags.add("猿臂比惊人");
            comment.append("您的上肢伸展潜力极大，具有极佳的气动姿态适应力。");
        }
        if (vo.getFemurTibiaRatio() != null && vo.getFemurTibiaRatio() > 1.15) {
            tags.add("长股骨特征");
            comment.append("大腿比例显著，骑行中重心天然偏后，对座垫后移量(Setback)非常敏感。");
        }
        if (vo.getFlexibilityScore() != null) {
            if (vo.getFlexibilityScore() < 50) {
                tags.add("柔韧性受限");
                comment.append("核心灵活性一般，初期建议采用保守的车头高度，避免颈背部酸痛。");
            } else if (vo.getFlexibilityScore() > 80) {
                tags.add("极佳柔韧性");
                comment.append("身体开发程度高，可以尝试更激进的竞技落差设定。");
            }
        }
        if (entity.getThoracicPainLevel() != null && entity.getThoracicPainLevel() > 0) {
            tags.add("痛感预警");
            comment.append("当前测试存在痛感，系统将自动下调 Fitting 激进程度，以康复保护为主。");
        }

        vo.setRiderTags(tags);
        vo.setExpertComment(comment.toString());
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
            return "Fittor 提示：由于历史数据或当前数据中的机能测试不完整，暂无法生成深度进化评估。建议完整录入“身体图谱”中的三项柔韧性测试。";
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
