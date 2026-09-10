package com.johnnylin.dev.common;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.*;

/**
 * 统一 API 响应包装类与全局异常处理器定义。
 *
 * <p>规范化 RESTful 接口的标准响应结构，包括业务状态码 {@code code}、
 * 提示信息 {@code message} 以及携带的业务数据实体 {@code data}。
 *
 * @param <T> 数据负载泛型类型
 * @param code 响应状态码，成功时为 "OK"，错误时格式为 "HTTP_{status}"
 * @param message 友好提示信息
 * @param data 响应业务数据负载
 */
public record Api<T>(String code, String message, T data) {

    /**
     * 构建成功的统一响应实体。
     *
     * @param <T> 数据类型
     * @param data 成功返回的数据负载
     * @return 包含 "OK" 状态码与数据的统一响应对象
     */
    public static <T> Api<T> ok(T data) {
        return new Api<>("OK", "成功", data);
    }

    /**
     * 创建 400 客户端参数错误的业务异常。
     *
     * @param message 错误描述信息
     * @return 400 状态码的 Failure 异常实例
     */
    public static Failure bad(String message) {
        return new Failure(400, message);
    }

    /**
     * 创建 404 资源未找到的业务异常。
     *
     * @return 404 状态码的 Failure 异常实例
     */
    public static Failure missing() {
        return new Failure(404, "记录不存在");
    }

    /**
     * 创建 409 业务状态冲突的业务异常（如并发版本冲突、外键依赖约束）。
     *
     * @param message 冲突原因描述
     * @return 409 状态码的 Failure 异常实例
     */
    public static Failure conflict(String message) {
        return new Failure(409, message);
    }

    /**
     * 业务逻辑处理失败异常类。
     *
     * <p>携带 HTTP 状态码，由全局异常处理器捕获并转化为符合 API 格式的 JSON 响应。
     */
    public static class Failure extends RuntimeException {
        /** HTTP 响应状态码（如 400、401、403、404、409） */
        public final int status;

        /**
         * 构造业务失败异常。
         *
         * @param status HTTP 状态码
         * @param message 错误提示信息
         */
        public Failure(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /**
     * 全局控制器异常切面处理器。
     *
     * <p>统一拦截所有控制器抛出的已知业务异常及常见 Spring Web 异常，
     * 转换为统一的 JSON 错误响应体，并注入链路追踪 {@code requestId}。
     */
    @RestControllerAdvice
    public static class Errors {
        private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Errors.class);

        /**
         * 构造标准 JSON 错误响应。
         *
         * @param status HTTP 状态码
         * @param message 错误提示信息
         * @return 封装了错误体及对应状态码的 ResponseEntity
         */
        private ResponseEntity<?> response(int status, String message) {
            var body = new LinkedHashMap<String, Object>();
            body.put("code", "HTTP_" + status);
            body.put("message", message);
            body.put("data", null);
            body.put("requestId", UUID.randomUUID().toString());
            return ResponseEntity.status(status).body(body);
        }

        /**
         * 捕获已知的业务处理失败异常。
         *
         * @param e 业务失败异常
         * @return 对应的错误响应
         */
        @ExceptionHandler(Failure.class)
        ResponseEntity<?> known(Failure e) {
            return response(e.status, e.getMessage());
        }

        /**
         * 捕获参数反序列化、格式匹配、缺失或非法参数等客户端输入异常。
         *
         * @param e 输入参数异常
         * @return 400 状态码错误响应
         */
        @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
                MissingServletRequestParameterException.class, IllegalArgumentException.class})
        ResponseEntity<?> invalid(Exception e) {
            return response(400, "请求格式或参数不正确");
        }

        /**
         * 捕获数据库完整性约束冲突异常（如唯一索引重复、外键关联失效）。
         *
         * @param e 数据完整性违规异常
         * @return 409 状态码错误响应
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        ResponseEntity<?> duplicate(Exception e) {
            return response(409, "编号重复或关联数据不满足约束");
        }

        /**
         * 捕获访问权限不足异常。
         *
         * @param e 权限拒绝异常
         * @return 403 状态码错误响应
         */
        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        ResponseEntity<?> denied(Exception e) {
            return response(403, "无权执行此操作");
        }

        /**
         * 捕获静态资源或接口路由不存在异常。
         *
         * @param e 资源未找到异常
         * @return 404 状态码错误响应
         */
        @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
        ResponseEntity<?> notFound(Exception e) {
            return response(404, "接口不存在");
        }

        /**
         * 捕获 HTTP 请求方式不支持异常（例如 POST 请求使用了 GET）。
         *
         * @param e 请求方法不支持异常
         * @return 405 状态码错误响应
         */
        @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
        ResponseEntity<?> method(Exception e) {
            return response(405, "请求方法不支持");
        }

        /**
         * 捕获未预期的系统兜底异常。
         *
         * @param e 未知系统异常
         * @return 500 状态码统一报错
         */
        @ExceptionHandler(Exception.class)
        ResponseEntity<?> unexpected(Exception e) {
            log.error("Request failed: {}", e.getClass().getSimpleName(), e);
            return response(500, "服务暂时不可用，请稍后重试");
        }
    }
}
