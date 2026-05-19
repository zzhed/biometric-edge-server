package com.biometric.exception;

import com.biometric.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.error("异常信息：{}",e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    /**
     * 处理参数校验异常
     * 当 @Valid 或 @Validated 校验失败时触发
     *
     * @param e 方法参数校验异常，包含字段错误信息
     * @return 统一响应结果，包含 400 错误码和校验失败详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.error(400, message);
    }
    /**
     * 处理未捕获的通用异常
     * 作为兜底异常处理器，捕获所有未被其他处理器匹配的异常
     *
     * @param e 异常对象，包含错误堆栈信息
     * @return 统一响应结果，包含 500 错误码和通用错误消息
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Result.error(500, "服务器内部错误");
    }
}
