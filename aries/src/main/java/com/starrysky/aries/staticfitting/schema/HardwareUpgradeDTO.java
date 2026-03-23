package com.starrysky.aries.staticfitting.schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 硬件升级防坑建议DTO
 */
@Setter
@Getter
public class HardwareUpgradeDTO {
    private String part;         // 部件 (如: 曲柄长度)
    private String currentValue; // 当前规格
    private String targetSpec;   // 建议规格
    private String reason;       // 推荐理由
}
