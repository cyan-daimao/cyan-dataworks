package com.cyan.dataworks.adapter.http.advice;

import com.cyan.arch.common.api.*;
import com.cyan.arch.common.util.CollUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * 全局异常处理器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 404 接口不存在异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Response<?> handleNoHandlerFoundException(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if ("/favicon.ico".equals(requestUri)) {
            log.debug("浏览器请求 favicon.ico 无匹配处理器，忽略");
        } else {
            log.warn("不存在Http接口 [{}][{}] ", request.getMethod(), requestUri);
        }
        return Response.failed(ErrorCode.NOT_FOUND, "接口不存在");
    }

    /**
     * 处理参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response<String> handleMissingParam(HttpServletRequest request, MissingServletRequestParameterException ex) {
        log.error("接口 [{}][{}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return Response.failed(ErrorCode.VALIDATE_FAILED, "参数%s不能为空".formatted(ex.getParameterName()));
    }

    /**
     * 处理参数校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<?> handleValidationError(HttpServletRequest request, MethodArgumentNotValidException ex) {
        List<String> errorInformation = ex.getBindingResult().getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .toList();
        String message = CollUtils.isEmpty(errorInformation) ? "" : errorInformation.getFirst();
        log.error("接口 [{}][{}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return Response.failed(ErrorCode.VALIDATE_FAILED, message);
    }

    /**
     * 处理 SilentException（业务静默异常）
     */
    @ExceptionHandler(SilentException.class)
    public Response<String> handleSilentException(HttpServletRequest request, SilentException ex) {
        log.error("接口 [{}][{}] SilentException: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return Response.failed(ErrorCode.FAILED, ex.getMsg());
    }

    /**
     * 处理 BusinessException（业务异常）
     */
    @ExceptionHandler(BusinessException.class)
    public Response<String> handleBusinessException(HttpServletRequest request, BusinessException ex) {
        log.error("接口 [{}][{}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return Response.failed(ErrorCode.FAILED, ex.getMsg());
    }

    /**
     * 处理 BaseException（基础异常）
     */
    @ExceptionHandler(BaseException.class)
    public Response<?> handleBaseException(HttpServletRequest request, BaseException ex) {
        log.error("接口 [{}][{}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return new Response<>(ex.getCode(), ex.getMsg(), ex.getDetail(), null);
    }

    /**
     * 处理通用 Exception
     */
    @ExceptionHandler(Exception.class)
    public Response<?> handleException(HttpServletRequest request, Exception ex) {
        log.error("接口 [{}][{}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return new Response<>(ErrorCode.FAILED.getCode(), ex.getMessage(), null, null);
    }
}
