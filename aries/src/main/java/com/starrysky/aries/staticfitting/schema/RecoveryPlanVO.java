package com.starrysky.aries.staticfitting.schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 训练与康复动作建议DTO
 * 用于给前端推送相对应的拉伸与强化计划
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/15 22:23
 */
@Setter
@Getter
public class RecoveryPlanVO {
    private String exerciseId;     // 动作库ID (如: stretch_01)
    private String title;          // 动作名称 (如: 髂胫束/臀部深度拉伸)
    private String desc;           // 指导描述
    private String gifUrl;         // 缩略图/动图URL
}
