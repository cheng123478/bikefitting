package com.starrysky.aries.user.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * WeChatSessionResponse
 * 微信登录返回值
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/20 12:46
 */
@Setter
@Getter
public class WeChatSessionResponseVO {
    @JsonProperty("openid")
    private String openid;
    @JsonProperty("session_key")
    private String sessionKey;
    @JsonProperty("unionid")
    private String unionid; // 只有在开放平台绑定了才会返回
    @JsonProperty("errcode")
    private Integer errCode;
    @JsonProperty("errmsg")
    private String errMsg;
}
