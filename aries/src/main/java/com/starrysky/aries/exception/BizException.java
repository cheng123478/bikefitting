package com.starrysky.aries.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * BizException
 * 业务异常类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Setter
@Getter
public class BizException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7013931856650729663L;

    private Integer code;
    private String message;

    public BizException() {
        super();
    }

    public BizException(String message) {
        super(message);
        this.code = 400; // 默认业务异常码为400 Bad Request
        this.message = message;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BizException(String message, Throwable throwable) {
        super(message, throwable);
        this.message = message;
    }

    public BizException(Integer code, String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
        this.message = message;
    }
}
