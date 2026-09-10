package com.johnnylin.dev.common;

/**
 * 统一 API 响应包装类。
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
}
