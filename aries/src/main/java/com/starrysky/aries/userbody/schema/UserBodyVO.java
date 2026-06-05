package com.starrysky.aries.userbody.schema;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户身体信息响应VO
 */
@Setter
@Getter
public class UserBodyVO {
    private Long id;
    private Long userId;
    private LocalDateTime createTime;

    private Byte gender;
    private Integer age;
    private Integer height;
    private Integer spanHeight;
    private Integer breastHeight;
    private Integer shoulderWidth;
    private Integer longArmLength;
    private Integer shortArmLength;
    private Integer thighLength;
    private Integer calfLength;
    private Integer ischialDistance;
    private Integer footLength;
    private Double weightKg;
    private Integer waistCircumference;
    private Double armRaiseAngle;
    private Double thoracicRotationAngle;
    private Integer thoracicPainLevel;
    private Double squatDepth;
    private Integer squatStabilityScore;
    private Integer plankScore;

    private Double apeIndex;
    private Double femurTibiaRatio;
    private Double trunkRatio;
    private Double bmi;
    private Double waistToHeightRatio;
    private List<String> riderTags;
    private String summary;
    private List<String> findings;
    private List<String> advice;
    private Integer flexibilityScore;
    private Double coreScore;
    private Integer stabilityScore;

    private String recSaddleHeight;
    private String recSaddleType;
    private String recStack;
    private String recReach;
}
