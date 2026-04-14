package com.starrysky.aries.userbody.schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 用户身体数据模型
 * 用于创建用户身体数据
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 15:37
 */
@Setter
@Getter
public class UserBodyDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Past(message = "出生日期不能晚于今天")
    private LocalDate birthday;

    private Byte gender;

    // 基础尺寸 (mm)
    @NotNull(message = "身高不能为空")
    @Min(value = 500, message = "身高数据异常(单位:mm)")
    @Max(value = 2500, message = "身高数据异常(单位:mm)")
    private Integer height;

    @Positive(message = "跨高必须为正数")
    private Integer spanHeight;
    @Positive(message = "胸高必须为正数")
    private Integer breastHeight;
    @Positive(message = "肩宽必须为正数")
    private Integer shoulderWidth;
    @Positive(message = "大臂长必须为正数")
    private Integer longArmLength;
    @Positive(message = "小臂长必须为正数")
    private Integer shortArmLength;
    @Positive(message = "大腿长必须为正数")
    private Integer thighLength;
    @Positive(message = "小腿长必须为正数")
    private Integer calfLength;
    @Positive(message = "坐骨间距必须为正数")
    private Integer ischialDistance;
    @Positive(message = "脚长必须为正数")
    private Integer footLength;

    // 灵活性与稳定性测试数据
    @Min(value = 0, message = "举臂角度不能小于0")
    @Max(value = 180, message = "举臂角度不能大于180")
    private Double armRaiseAngle;

    @Min(value = 0, message = "胸椎旋转角度不能小于0")
    @Max(value = 90, message = "胸椎旋转角度不能大于90")
    private Double thoracicRotationAngle;

    @Min(value = 0, message = "胸椎痛感等级最小为0")
    @Max(value = 5, message = "胸椎痛感等级最大为5")
    private Integer thoracicPainLevel;

    private Double squatDepth;

    @Min(value = 1, message = "深蹲稳定性评分最小为1")
    @Max(value = 5, message = "深蹲稳定性评分最大为5")
    private Integer squatStabilityScore;

    @Min(value = 1, message = "平板支撑评分最小为1")
    @Max(value = 5, message = "平板支撑评分最大为5")
    private Integer plankScore;
}
