package com.starrysky.aries.membership.exception;

/**
 * 会员与权限业务异常
 *
 * @author adi
 */
public class MembershipException extends RuntimeException {
    
    private final Integer code;

    public MembershipException(String message) {
        super(message);
        this.code = 403; // Default forbidden/insufficient privilege code
    }

    public MembershipException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
