package com.starrysky.aries.sms.record;

import java.util.Map;

/**
 * 短信服务-响应参数聚合
 *
 * @param errorCode 错误码
 * @param reason    错误原因
 * @param result    结果数据
 */
public record SmsResponse(int errorCode, String reason, Map<String, Object> result) {
    public boolean isSuccess() {
        return errorCode == 0;
    }
}
