package com.starrysky.aries.peloton.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PelotonStatsVO
 * 破风社首页统计信息视图对象
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PelotonStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 车队总数 */
    private Long totalTeams;

    /** 已加入车队的骑友总数（去重） */
    private Long totalMembers;

    /** 覆盖城市数（去重） */
    private Long totalCities;
}
