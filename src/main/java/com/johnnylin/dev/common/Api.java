package com.johnnylin.dev.common;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.*;

public record Api<T>(String code, String message, T data) {
    public static <T> Api<T> ok(T data) { return new Api<>("OK", "成功", data); }
    public static Failure bad(String message) { return new Failure(400, message); }
    public static Failure missing() { return new Failure(404, "记录不存在"); }
    public static Failure conflict(String message) { return new Failure(409, message); }
    public static class Failure extends RuntimeException {
        public final int status;
        public Failure(int status, String message) { super(message); this.status = status; }
    }
    @RestControllerAdvice
    public static class Errors {
        private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Errors.class);
        private ResponseEntity<?> response(int status, String message) {
            var body=new LinkedHashMap<String,Object>();body.put("code","HTTP_"+status);body.put("message",message);body.put("data",null);body.put("requestId",UUID.randomUUID().toString());
            return ResponseEntity.status(status).body(body);
        }
        @ExceptionHandler(Failure.class)
        ResponseEntity<?> known(Failure e) { return response(e.status,e.getMessage()); }
        @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
                MissingServletRequestParameterException.class, IllegalArgumentException.class})
        ResponseEntity<?> invalid(Exception e) { return response(400,"请求格式或参数不正确"); }
        @ExceptionHandler(DataIntegrityViolationException.class)
        ResponseEntity<?> duplicate(Exception e) { return response(409,"编号重复或关联数据不满足约束"); }
        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        ResponseEntity<?> denied(Exception e) { return response(403,"无权执行此操作"); }
        @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
        ResponseEntity<?> notFound(Exception e) { return response(404,"接口不存在"); }
        @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
        ResponseEntity<?> method(Exception e) { return response(405,"请求方法不支持"); }
        @ExceptionHandler(Exception.class)
        ResponseEntity<?> unexpected(Exception e) { log.error("Request failed: {}",e.getClass().getSimpleName()); return response(500,"服务暂时不可用，请稍后重试"); }
    }
}
