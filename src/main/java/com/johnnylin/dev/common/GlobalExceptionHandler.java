package com.johnnylin.dev.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * 全局统一异常处理器。
 *
 * <p>统一拦截所有 Controller 层抛出的异常，统一封装为标准 API 响应结构：
 * <ul>
 *   <li>已知业务异常 {@link Api.Failure}：提取其内部定义的 HTTP 状态码与业务说明</li>
 *   <li>参数校验与转换异常（反序列化失败、缺少必填参数、参数格式不符等）：归类为 400</li>
 *   <li>数据库完整性约束冲突（主键/唯一索引重复、外键引用受阻）：归类为 409</li>
 *   <li>安全访问拒绝（权限不足）：归类为 403</li>
 *   <li>资源/路由不存在（404）与请求方法不支持（405）</li>
 *   <li>未预期的系统兜底异常：记录错误日志并返回 500 友好提示，附带链路跟踪 ID</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 构造标准 JSON 错误响应体。
     *
     * @param status HTTP 状态码
     * @param message 错误描述信息
     * @return 封装了标准错误结构的 ResponseEntity
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
     * 捕获业务显式抛出的失败异常。
     *
     * @param e 业务失败异常
     * @return 对应状态码的统一错误响应
     */
    @ExceptionHandler(Api.Failure.class)
    public ResponseEntity<?> handleFailure(Api.Failure e) {
        return response(e.status, e.getMessage());
    }

    /**
     * 捕获客户端输入参数不合法、格式错误或反序列化失败异常。
     *
     * @param e 参数相关异常
     * @return 400 状态码统一错误响应
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<?> handleInvalidParameters(Exception e) {
        return response(400, "请求格式或参数不正确");
    }

    /**
     * 捕获数据库持久层完整性违规异常（如唯一索引重复、外键受限）。
     *
     * @param e 数据完整性违规异常
     * @return 409 状态码统一错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(Exception e) {
        return response(409, "编号重复或关联数据不满足约束");
    }

    /**
     * 捕获访问权限不足异常。
     *
     * @param e 访问拒绝异常
     * @return 403 状态码统一错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(Exception e) {
        return response(403, "无权执行此操作");
    }

    /**
     * 捕获静态资源或接口路由不存在异常。
     *
     * @param e 资源未找到异常
     * @return 404 状态码统一错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(Exception e) {
        return response(404, "接口不存在");
    }

    /**
     * 捕获不支持的 HTTP 请求方法异常。
     *
     * @param e 请求方法不支持异常
     * @return 405 状态码统一错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(Exception e) {
        return response(405, "请求方法不支持");
    }

    /**
     * 捕获未预期的系统全局兜底异常。
     *
     * @param e 未知异常
     * @return 500 状态码统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpectedException(Exception e) {
        log.error("Unhandled server exception: {}", e.getClass().getSimpleName(), e);
        return response(500, "服务暂时不可用，请稍后重试");
    }
}
