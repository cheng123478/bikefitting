package com.starrysky.aries.staticfitting.schema;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 骑行诱发疼痛类型枚举
 * 用于记录受限于当前车辆设定的动态疼痛特征
 *
 * @author adi
 */
@Getter
public enum RidingPainType {
    KNEE_ANTERIOR("knee_anterior", "膝盖前侧痛"),
    KNEE_POSTERIOR("knee_posterior", "膝盖后侧痛"),
    HAND_NUMBNESS("hand_numbness", "手掌发麻/刺痛"),
    LOWER_BACK("lower_back", "腰背酸痛"),
    PERINEUM_PAIN("perineum_pain", "会阴/坐骨痛");

    @JsonValue
    private final String code;
    private final String description;

    RidingPainType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    // 提供一个根据 code 找 Enum 的方法，转换器会用到
    public static RidingPainType fromCode(String code) {
        for (RidingPainType type : RidingPainType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的痛点代码: " + code);
    }
}
