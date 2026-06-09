package com.starrysky.aries.membership.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会员类型枚举
 *
 * @author adi
 */
@Getter
@AllArgsConstructor
public enum MemberTypeEnum {
    FREE(0, "普通免费用户"),
    PRO(1, "PRO会员");

    private final Integer code;
    private final String description;
}
