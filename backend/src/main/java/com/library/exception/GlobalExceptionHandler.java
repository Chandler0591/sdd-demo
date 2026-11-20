package com.library.exception;

import com.easy.query.core.exception.EasyQuerySQLCommandException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理：业务错误返回 4xx + 中文 message，前端直接展示。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(EasyQuerySQLCommandException.class)
    public ResponseEntity<Map<String, Object>> handleEasyQuerySQL(EasyQuerySQLCommandException e) {
        log.warn("数据库操作失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "数据冲突，请检查输入是否重复"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.internalServerError().body(Map.of("message", "服务器内部错误"));
    }
}
