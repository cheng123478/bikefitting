package com.starrysky.aries.userbody.schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 用户身体数据录入 DTO
 */
@Setter
@Getter
public class UserBodyDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Past(message = "出生日期不能晚于今天")
    private LocalDate birthday;

    private Byte gender;

    @NotNull(message = "身高不能为空")
    @Min(value = 1200, message = "身高数据异常(单位:mm，正常范围1200-2200)")
    @Max(value = 2200, message = "身高数据异常(单位:mm，正常范围1200-2200)")
    private Integer height;

    @Min(value = 600, message = "跨高数据异常(单位:mm，正常范围600-1100)")
    @Max(value = 1100, message = "跨高数据异常(单位:mm，正常范围600-1100)")
    private Integer spanHeight;
    @Min(value = 900, message = "胸高数据异常(单位:mm，正常范围900-1800)")
    @Max(value = 1800, message = "胸高数据异常(单位:mm，正常范围900-1800)")
    private Integer breastHeight;
    @Min(value = 200, message = "肩宽数据异常(单位:mm，正常范围200-600)")
    @Max(value = 600, message = "肩宽数据异常(单位:mm，正常范围200-600)")
    private Integer shoulderWidth;
    @Min(value = 180, message = "大臂长数据异常(单位:mm，正常范围180-500)")
    @Max(value = 500, message = "大臂长数据异常(单位:mm，正常范围180-500)")
    private Integer longArmLength;
    @Min(value = 160, message = "小臂长数据异常(单位:mm，正常范围160-450)")
    @Max(value = 450, message = "小臂长数据异常(单位:mm，正常范围160-450)")
    private Integer shortArmLength;
    @Min(value = 250, message = "大腿长数据异常(单位:mm，正常范围250-650)")
    @Max(value = 650, message = "大腿长数据异常(单位:mm，正常范围250-650)")
    private Integer thighLength;
    @Min(value = 250, message = "小腿长数据异常(单位:mm，正常范围250-650)")
    @Max(value = 650, message = "小腿长数据异常(单位:mm，正常范围250-650)")
    private Integer calfLength;
    @Min(value = 50, message = "坐骨间距数据异常(单位:mm，正常范围50-200)")
    @Max(value = 200, message = "坐骨间距数据异常(单位:mm，正常范围50-200)")
    private Integer ischialDistance;
    @Min(value = 180, message = "足长数据异常(单位:mm，正常范围180-350)")
    @Max(value = 350, message = "足长数据异常(单位:mm，正常范围180-350)")
    private Integer footLength;
    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "30.0", message = "体重数据异常(单位:kg，正常范围30-250)")
    @DecimalMax(value = "250.0", message = "体重数据异常(单位:kg，正常范围30-250)")
    private Double weightKg;
    @Min(value = 400, message = "腰围数据异常(单位:mm，正常范围400-1800)")
    @Max(value = 1800, message = "腰围数据异常(单位:mm，正常范围400-1800)")
    private Integer waistCircumference;

    @Min(value = 0, message = "举臂角度不能小于0")
    @Max(value = 180, message = "举臂角度不能大于180")
    private Double armRaiseAngle;

    @Min(value = 0, message = "胸椎旋转角度不能小于0")
    @Max(value = 90, message = "胸椎旋转角度不能大于90")
    private Double thoracicRotationAngle;

    @Min(value = 0, message = "胸椎疼痛等级最小为0")
    @Max(value = 5, message = "胸椎疼痛等级最大为5")
    private Integer thoracicPainLevel;

    private Double squatDepth;

    @Min(value = 1, message = "深蹲稳定性评分最小为1")
    @Max(value = 5, message = "深蹲稳定性评分最大为5")
    private Integer squatStabilityScore;

    @Min(value = 1, message = "平板支撑评分最小为1")
    @Max(value = 5, message = "平板支撑评分最大为5")
    private Integer plankScore;
}
