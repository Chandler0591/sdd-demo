package com.library.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：携带中文提示信息与 HTTP 状态码，由全局异常处理器统一转换为 4xx 响应。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
