package com.starrysky.aries.sms.record;

import java.util.Map;

public record SmsResponse(int errorCode, String reason, Map<String, Object> result) {
    public boolean isSuccess() {
        return errorCode == 0;
    }
}
