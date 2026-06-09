package com.starrysky.aries.staticfitting.schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 硬件升级防坑建议DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/15 22:23
 */
@Setter
@Getter
public class HardwareUpgradeVO {
    private String part;         // 部件 (如: 曲柄长度)
    private String currentValue; // 当前规格
    private String targetSpec;   // 建议规格
    private String reason;       // 推荐理由
}
