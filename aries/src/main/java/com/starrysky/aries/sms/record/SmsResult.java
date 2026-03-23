package com.starrysky.aries.sms.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record SmsResult(boolean success, @JsonIgnore String code, String message) {
}
