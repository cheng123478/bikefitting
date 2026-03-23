package com.starrysky.aries.staticfitting.service;

import com.starrysky.aries.entity.UserBike;
import com.starrysky.aries.entity.UserBikeSetup;
import com.starrysky.aries.entity.DiagnosticDetail;
import com.starrysky.aries.entity.StaticFittingResult;
import com.starrysky.aries.entity.UserBody;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fittor诊断文案库管理类
 * 负责生成基于生物力学的诊断条目
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/4 22:15
 */
@Component
public class FittorDiagnosticsManager {
    @Resource
    private DiagnosticLibrary diagLib;

    /**
     * 1. 生成身体画像诊断
     * 依赖：height, longArmLength (必填), femurRatio (选填), thoracicScore (选填)
     */
    public void diagnosePortrait(StaticFittingResult res, UserBody userBody, double apeIndex, double femurRatio) {
        Map<String, Object> v = new HashMap<>();

        // 猿臂比诊断：依赖手臂长度和身高
        if (userBody.getLongArmLength() != null && userBody.getHeight() != null) {
            v.put("armTotal",
                    userBody.getLongArmLength() + (userBody.getShortArmLength() != null ? userBody.getShortArmLength()
                            : userBody.getLongArmLength()));
            v.put("height", userBody.getHeight());
            String apeKey = apeIndex > 1.04 ? "ape_index_high"
                    : (apeIndex < 0.97 ? "ape_index_low" : "ape_index_standard");
            addDetail(res, "身体画像", "猿臂比例", diagLib.getTemplate("module_1_portrait", apeKey, v));
        }

        // 长股骨诊断：正常比例 0.45 - 0.47
        if (femurRatio > 0) {
            v.put("ratio", String.format("%.2f", femurRatio));
            if (femurRatio > 0.47) {
                addDetail(res, "身体画像", "发力特性", diagLib.getTemplate("module_1_portrait", "femur_ratio_high", v));
            } else if (femurRatio < 0.45) {
                addDetail(res, "身体画像", "发力特性", "大腿部骨骼比例偏短。");
            }
        }

        // 柔韧性画像：只有算出了分数才展示
        if (res.getThoracicScore() != null) {
            v.put("score", res.getThoracicScore());
            if (res.getThoracicScore() > 85) {
                addDetail(res, "身体画像", "柔韧素质", diagLib.getTemplate("module_1_portrait", "flexibility_top", v));
            }
        }
    }

    /**
     * 2. 下肢动力链诊断
     * 依赖：recSeatHeight (基准), femurRatio (比例修正), curSeatHeight (现状对比)
     */
    public void diagnoseLowerBody(StaticFittingResult res, double femurRatio, Integer curSeatHeight) {
        Map<String, Object> v = new HashMap<>();
        StringBuilder logic = new StringBuilder();

        // A. 基础座高建议 (Level 1 必出)
        if (res.getRecSeatHeight() != null) {
            v.put("recValue", res.getRecSeatHeight());
            logic.append(diagLib.getTemplate("module_2_lower_body", "seat_height_standard", v));
        }

        // B. 现状偏差预警 (仅当有实车数据时)
        if (curSeatHeight != null && curSeatHeight > 0 && res.getRecSeatHeight() != null) {
            int diff = Math.abs(curSeatHeight - res.getRecSeatHeight());
            if (diff > 10) {
                v.put("diff", diff);
                logic.append("\n").append(diagLib.getTemplate("module_2_lower_body", "seat_height_warning", v));
            }
        }

        // C. 后移量建议 (仅当有比例数据时)
        if (femurRatio > 0 && res.getRecSetback() != null) {
            v.put("ratio", String.format("%.2f", femurRatio));
            v.put("recValue", res.getRecSetback());
            logic.append("\n").append(StaticFittingCalculator.calcSaddleSetbackAdvice(femurRatio));
        }

        // D. 锁片建议 (通用生物力学常识)
        logic.append("\n").append(diagLib.getTemplate("module_2_lower_body", "cleat_advice", v));

        addDetail(res, "下肢动力链", "踩踏力线", logic.toString());
    }

    /**
     * 3. 上肢座舱诊断
     * 依赖：Reach+/Stack+/Drop (空间坐标), 灵活性角度 (柔韧性)
     */
    public void diagnoseUpperBody(StaticFittingResult res, UserBody userBody) {
        Map<String, Object> v = new HashMap<>();
        StringBuilder logic = new StringBuilder();

        // A. 空间坐标解释 (只有算出了建议值才显示)
        if (res.getRecStackPlus() != null && res.getRecReachPlus() != null) {
            v.put("stackPlus", res.getRecStackPlus());
            v.put("reachPlus", res.getRecReachPlus());
            v.put("recValue", res.getRecHandlebarDrop() != null ? res.getRecHandlebarDrop() : "--");
            logic.append(diagLib.getTemplate("module_3_upper_body", "space_logic", v));
            
            if (res.getRecHandlebarDrop() != null) {
                logic.append("\n").append(diagLib.getTemplate("module_3_upper_body", "drop_logic", v));
            }
        }

        // B. 柔韧性与痛感分析
        Integer pain = userBody.getThoracicPainLevel();
        if (pain != null && pain > 0) {
            v.put("level", pain);
            logic.append("\n").append(diagLib.getTemplate("module_5_safety", "pain_alert", v));
        } else if (res.getThoracicScore() != null) {
            v.put("score", res.getThoracicScore());
            if (res.getThoracicScore() < 60) {
                logic.append("\n").append(diagLib.getTemplate("module_4_compensation", "thoracic_restricted", v));
            } else {
                logic.append("\n").append(diagLib.getTemplate("module_4_compensation", "thoracic_excellent", v));
            }
        }

        // C. 手变头通用建议
        logic.append("\n").append(diagLib.getTemplate("module_3_upper_body", "hoods_angle", v));

        if (logic.length() > 0) {
            addDetail(res, "上肢座舱", "舒适度/落差", logic.toString());
        }
    }

    /**
     * 4. 器材规格选型
     * 依赖：recCrankLength, ischialDistance, shoulderWidth
     */
    public void diagnoseComponents(StaticFittingResult res, UserBody userBody) {
        Map<String, Object> v = new HashMap<>();
        StringBuilder logic = new StringBuilder();

        // 曲柄规格
        if (res.getRecCrankLength() != null) {
            v.put("recValue", res.getRecCrankLength());
            logic.append(diagLib.getTemplate("module_2_lower_body", "crank_logic", v));
        }

        // 座垫规格：依赖坐骨间距
        if (userBody.getIschialDistance() != null && userBody.getIschialDistance() > 0) {
            v.put("value", userBody.getIschialDistance());
            logic.append("\n").append(diagLib.getTemplate("module_6_equipment", "saddle_logic", v));
        }

        // 把横规格：依赖肩宽
        if (res.getRecHandlebarWidth() != null) {
            v.put("recValue", res.getRecHandlebarWidth());
            logic.append("\n").append(diagLib.getTemplate("module_3_upper_body", "handlebar_width", v));
        }

        if (logic.length() > 0) {
            addDetail(res, "器材匹配", "规格选型", logic.toString());
        }
    }

    /**
     * 5. 实车调教方案 (现状对比)
     * 依赖：recStemLength
     */
    public void addAdjustmentAdvice(StaticFittingResult res, UserBike userBike, UserBikeSetup setup) {
        if (res.getRecStemLength() == null)
            return;

        Map<String, Object> v = new HashMap<>();
        v.put("recValue", res.getRecStemLength());

        String logic = diagLib.getTemplate("module_6_equipment", "stem_logic", v);
        logic += "\n" + diagLib.getTemplate("module_7_summary", "adjusting_mode", v);

        addDetail(res, "调教方案", "现状对比诊断", logic);
    }

    /**
     * 7. 安全稳定性诊断
     * 依赖：squatStabilityScore
     */
    public void diagnoseSafety(StaticFittingResult res, UserBody userBody) {
        Integer squat = userBody.getSquatStabilityScore();
        if (squat != null && squat < 3) {
            String logic = diagLib.getTemplate("module_5_safety", "stability_warning", new HashMap<>());
            addDetail(res, "稳定性", "核心支撑", logic);
        }
    }

    // --- 私有辅助 ---

    private void addDetail(StaticFittingResult res, String part, String feature, String logic) {
        if (logic == null || logic.trim().isEmpty())
            return;
        DiagnosticDetail detail = new DiagnosticDetail();
        detail.setPart(part);
        detail.setBodyFeature(feature);
        detail.setAdjustmentLogic(logic.trim());
        res.getDetails().add(detail);
        detail.setStaticFittingResult(res);
    }
}
