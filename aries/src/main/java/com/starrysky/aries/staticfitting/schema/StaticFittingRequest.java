package com.starrysky.aries.staticfitting.schema;

import com.starrysky.aries.entity.UserBike;
import com.starrysky.aries.entity.UserBody;
import com.starrysky.aries.util.RidingPainType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 静态Fitting请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/5 16:11
 */
@Setter
@Getter
public class StaticFittingRequest {
    private UserBody userBody;
    private UserBike userBike;
    // 疼痛情况
    private List<RidingPainType> painLevels;
}
