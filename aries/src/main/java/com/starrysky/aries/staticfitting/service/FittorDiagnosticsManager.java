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
     * 生成身体画像诊断
     * 依赖：height, longArmLength (必填), femurRatio (选填), thoracicScore (选填)
     *
     * @param res        静态分析结果
     * @param userBody   用户身体信息
     * @param apeIndex   猿臂比
     * @param femurRatio 大腿骨比例
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
     * 下肢动力链诊断
     * 依赖：recSeatHeight (基准), femurRatio (比例修正), setup (现状对比)
     *
     * @param res        静态分析结果
     * @param femurRatio 大腿骨比例
     * @param setup      用户实车信息
     */
    public void diagnoseLowerBody(StaticFittingResult res, double femurRatio, UserBikeSetup setup) {
        Map<String, Object> v = new HashMap<>();
        StringBuilder logic = new StringBuilder();

        // A. 基础座高建议 (Level 1 必出)
        if (res.getRecSeatHeight() != null) {
            v.put("recValue", res.getRecSeatHeight());
            logic.append(diagLib.getTemplate("module_2_lower_body", "seat_height_standard", v));
        }

        // B. 现状偏差预警 (仅当有实车数据时)
        if (setup != null && setup.getSaddleHeight() != null && res.getRecSeatHeight() != null) {
            int curHeight = setup.getSaddleHeight();
            int diff = res.getRecSeatHeight() - curHeight;
            if (Math.abs(diff) > 5) {
                v.put("diff", Math.abs(diff));
                v.put("action", diff > 0 ? "调高" : "调低");
                logic.append("\n").append("【调教建议】：当前座高比理想值").append(diff > 0 ? "偏低 " : "偏高 ")
                        .append(Math.abs(diff)).append("mm。具体建议：请将座垫").append(diff > 0 ? "升高 " : "降低 ")
                        .append(Math.abs(diff)).append("mm。");
            }
        }

        // C. 后移量建议与对比
        if (femurRatio > 0 && res.getRecSetback() != null) {
            v.put("ratio", String.format("%.2f", femurRatio));
            v.put("recValue", res.getRecSetback());
            logic.append("\n").append(StaticFittingCalculator.calcSaddleSetbackAdvice(femurRatio));

            if (setup != null && setup.getSaddleSetback() != null) {
                int diff = res.getRecSetback() - setup.getSaddleSetback();
                if (Math.abs(diff) > 5) {
                    logic.append(" 当前 Setback 偏").append(diff > 0 ? "小" : "大")
                            .append("，建议座垫向").append(diff > 0 ? "后移 " : "前移 ")
                            .append(Math.abs(diff)).append("mm。");
                }
            }
        }

        // D. 锁片建议
        logic.append("\n").append(diagLib.getTemplate("module_2_lower_body", "cleat_advice", v));

        addDetail(res, "下肢动力链", "踩踏力线", logic.toString());
    }

    /**
     * 上肢座舱诊断
     * 依赖：Reach+/Stack+/Drop (空间坐标), setup (现状对比)
     *
     * @param res      静态分析结果
     * @param userBody 用户身体信息
     * @param setup    用户实车信息
     */
    public void diagnoseUpperBody(StaticFittingResult res, UserBody userBody, UserBikeSetup setup) {
        Map<String, Object> v = new HashMap<>();
        StringBuilder logic = new StringBuilder();

        // A. 空间坐标解释
        if (res.getRecStackPlus() != null && res.getRecReachPlus() != null) {
            v.put("stackPlus", res.getRecStackPlus());
            v.put("reachPlus", res.getRecReachPlus());
            v.put("recValue", res.getRecHandlebarDrop() != null ? res.getRecHandlebarDrop() : "--");
            logic.append(diagLib.getTemplate("module_3_upper_body", "space_logic", v));

            if (res.getRecHandlebarDrop() != null) {
                logic.append("\n").append(diagLib.getTemplate("module_3_upper_body", "drop_logic", v));

                // 现状对比：落差
                if (setup != null && setup.getHandlebarDrop() != null) {
                    int diff = res.getRecHandlebarDrop() - setup.getHandlebarDrop();
                    if (Math.abs(diff) > 10) {
                        logic.append(" 当前落差比建议值").append(diff > 0 ? "偏小 " : "偏大 ")
                                .append(Math.abs(diff)).append("mm，请通过增减垫圈进行微调。");
                    }
                }
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

        if (!logic.isEmpty()) {
            addDetail(res, "上肢座舱", "舒适度/落差", logic.toString());
        }
    }

    /**
     * 器材规格选型
     *
     * @param res      静态分析结果
     * @param userBody 用户身体信息
     * @param setup    用户实车信息
     */
    public void diagnoseComponents(StaticFittingResult res, UserBody userBody, UserBikeSetup setup) {
        Map<String, Object> v = new HashMap<>();
        StringBuilder logic = new StringBuilder();

        // 曲柄规格对比
        if (res.getRecCrankLength() != null) {
            v.put("recValue", res.getRecCrankLength());
            logic.append(diagLib.getTemplate("module_2_lower_body", "crank_logic", v));
            if (setup != null && setup.getCrankLength() != null && !res.getRecCrankLength().equals(setup.getCrankLength())) {
                logic.append("【注意】：您当前实车使用的是 ").append(setup.getCrankLength())
                        .append("mm 曲柄，建议在下次升级时考虑更换。");
            }
        }

        // 座垫规格
        if (userBody.getIschialDistance() != null && userBody.getIschialDistance() > 0) {
            v.put("value", userBody.getIschialDistance());
            logic.append("\n").append(diagLib.getTemplate("module_6_equipment", "saddle_logic", v));
        }

        // 把横规格对比
        if (res.getRecHandlebarWidth() != null) {
            v.put("recValue", res.getRecHandlebarWidth());
            logic.append("\n").append(diagLib.getTemplate("module_3_upper_body", "handlebar_width", v));
            if (setup != null && setup.getHandlebarWidth() != null && !res.getRecHandlebarWidth().equals(setup.getHandlebarWidth())) {
                logic.append(" 当前把横宽度 (").append(setup.getHandlebarWidth()).append("mm) 与建议值不符。");
            }
        }

        if (!logic.isEmpty()) {
            addDetail(res, "器材匹配", "规格选型", logic.toString());
        }
    }

    /**
     * 实车调教方案 (现状对比)
     * 依赖：recStemLength, setup
     *
     * @param res      静态分析结果
     * @param userBike 用户单车信息
     * @param setup    用户实车信息
     */
    public void addAdjustmentAdvice(StaticFittingResult res, UserBike userBike, UserBikeSetup setup) {
        if (res.getRecStemLength() == null)
            return;

        Map<String, Object> v = new HashMap<>();
        v.put("recValue", res.getRecStemLength());

        StringBuilder logic = new StringBuilder();
        logic.append(diagLib.getTemplate("module_6_equipment", "stem_logic", v));

        // 核心对比逻辑：把立长度
        if (setup != null && setup.getStemLength() != null) {
            int diff = res.getRecStemLength() - setup.getStemLength();
            if (Math.abs(diff) >= 5) {
                logic.append("\n【调教详情】：您当前的把立长度为 ").append(setup.getStemLength())
                        .append("mm。建议更换为比当前").append(diff > 0 ? "长 " : "短 ")
                        .append(Math.abs(diff)).append("mm 的把立以达到理想前伸姿态。");
            } else {
                logic.append("\n【调教详情】：您当前的把立长度匹配良好。");
            }
        }

        logic.append("\n").append(diagLib.getTemplate("module_7_summary", "adjusting_mode", v));

        addDetail(res, "调教方案", "现状对比诊断", logic.toString());
    }


    /**
     * 安全稳定性诊断
     * 依赖：squatStabilityScore
     *
     * @param res      静态分析结果
     * @param userBody 用户身体信息
     */
    public void diagnoseSafety(StaticFittingResult res, UserBody userBody) {
        Integer squat = userBody.getSquatStabilityScore();
        if (squat != null && squat < 3) {
            String logic = diagLib.getTemplate("module_5_safety", "stability_warning", new HashMap<>());
            addDetail(res, "稳定性", "核心支撑", logic);
        }
    }

    /**
     * 添加诊断详情
     *
     * @param res     静态分析结果
     * @param part    部位
     * @param feature 特征
     * @param logic   调整逻辑
     */
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
