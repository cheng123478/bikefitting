package com.starrysky.aries.util;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 骑行诱发疼痛类型枚举
 * 用于记录受限于当前车辆设定的动态疼痛特征
 *
 * @author adi
 * @version 1.0.0
 * @since 2026-4-5 15:00:00
 */
@Getter
public enum RidingPainType {
    NECK_PAIN("neck_pain", "颈部酸痛/僵硬"),
    SHOULDER_PAIN("shoulder_pain", "肩部/斜方肌酸痛"),
    HAND_NUMBNESS("hand_numbness", "手掌发麻/刺痛"),
    LOWER_BACK("lower_back", "腰背酸痛"),
    PERINEUM_PAIN("perineum_pain", "会阴/坐骨痛"),
    KNEE_ANTERIOR("knee_anterior", "膝盖前侧痛"),
    KNEE_POSTERIOR("knee_posterior", "膝盖后侧痛"),
    KNEE_MEDIAL("knee_medial", "膝盖内侧痛"),
    KNEE_LATERAL("knee_lateral", "膝盖外侧痛"),
    FOOT_NUMBNESS("foot_numbness", "脚部发麻/灼热"),
    ACHILLES_PAIN("achilles_pain", "跟腱痛/足跟痛");

    @JsonValue
    private final String code;
    private final String description;

    RidingPainType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 提供一个根据 code 找 Enum 的方法，转换器会用到
     *
     * @param code 痛点代码
     * @return 痛点枚举
     */
    public static RidingPainType fromCode(String code) {
        for (RidingPainType type : RidingPainType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的痛点代码: " + code);
    }

    /**
     * 根据 code 获取描述文案，支持安全容错
     *
     * @param code 痛点代码
     * @return 描述文案
     */
    public static String getDescriptionByCode(String code) {
        for (RidingPainType type : RidingPainType.values()) {
            if (type.getCode().equals(code)) {
                return type.getDescription();
            }
        }
        return code; // 找不到则返回原始 code，保证基础可见性
    }
}
