package com.starrysky.aries.peloton.schema;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AddBikeToGarageDTO
 * 将单车录入车队车库请求参数
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Data
public class AddBikeToGarageDTO {

    /** 单车ID（必填） */
    @NotNull(message = "请提供单车ID")
    private Long bikeId;
}
