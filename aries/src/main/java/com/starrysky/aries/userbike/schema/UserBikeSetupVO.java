package com.starrysky.aries.userbike.schema;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户单车实际配置信息VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 22:44
 */
@Setter
@Getter
public class UserBikeSetupVO {
    private Long id;
    private Integer crankLength;
    private Integer stemLength;
    private Integer handlebarWidth;
    private Double stemAngle;
    private Integer spacersHeight;
    private Integer handlebarDrop;
    private Integer saddleWidth;
    private Integer saddleHeight;
    private Integer saddleSetback;
    private Double saddleTilt;
    private Integer pedalType;
    private LocalDateTime createTime;
}
