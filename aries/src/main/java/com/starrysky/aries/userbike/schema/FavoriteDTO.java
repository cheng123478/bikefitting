package com.starrysky.aries.userbike.schema;

import lombok.Data;

/**
 * 收藏请求DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@Data
public class FavoriteDTO {
    /**
     * 车型ID
     */
    private Long bikeModelId;
}
