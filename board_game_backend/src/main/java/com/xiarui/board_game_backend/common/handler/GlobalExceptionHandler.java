package com.xiarui.board_game_backend.common.handler;

import com.xiarui.board_game_backend.common.entity.Result;
import com.xiarui.board_game_backend.common.entity.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验异常.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manve) {
            message = extractFieldErrors(manve.getBindingResult().getFieldErrors());
        } else if (ex instanceof BindException bindException) {
            message = extractFieldErrors(bindException.getBindingResult().getFieldErrors());
        } else {
            message = "参数错误";
        }
        log.warn("参数校验失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 业务运行时异常.
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(ResultCode.ERROR.getCode(), ex.getMessage());
    }

    /**
     * 未知异常兜底.
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }

    private String extractFieldErrors(java.util.List<FieldError> errors) {
        return errors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
    }
}
