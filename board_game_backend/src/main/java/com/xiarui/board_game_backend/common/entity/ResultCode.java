package com.xiarui.board_game_backend.common.entity;

/**
 * 响应状态码枚举
 */
public enum ResultCode {
    
    /** 成功 */
    SUCCESS(200, "操作成功"),
    
    /** 失败 */
    ERROR(500, "操作失败"),
    
    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),
    
    /** 未登录 */
    UNAUTHORIZED(401, "未登录或登录已过期"),
    
    /** 无权限 */
    FORBIDDEN(403, "没有权限访问"),
    
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    
    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    /** 请求超时 */
    REQUEST_TIMEOUT(408, "请求超时"),
    
    /** 系统繁忙 */
    SYSTEM_BUSY(429, "系统繁忙，请稍后再试"),
    
    /** 系统错误 */
    SYSTEM_ERROR(500, "系统错误"),
    
    /** 用户名或密码错误 */
    USERNAME_PASSWORD_ERROR(1001, "用户名或密码错误"),
    
    /** 用户已存在 */
    USER_EXISTS(1002, "用户已存在"),
    
    /** 用户不存在 */
    USER_NOT_EXISTS(1003, "用户不存在"),
    
    /** 邮箱已存在 */
    EMAIL_EXISTS(1004, "邮箱已存在"),
    
    /** 验证码错误 */
    VERIFICATION_CODE_ERROR(1005, "验证码错误"),
    
    /** 验证码已过期 */
    VERIFICATION_CODE_EXPIRED(1006, "验证码已过期"),
    
    /** 邮箱验证失败 */
    EMAIL_VERIFICATION_FAILED(1007, "邮箱验证失败"),
    
    /** 密码重置失败 */
    PASSWORD_RESET_FAILED(1008, "密码重置失败"),
    
    /** 用户注册失败 */
    USER_REGISTER_FAILED(1009, "用户注册失败"),
    
    /** 用户登录失败 */
    USER_LOGIN_FAILED(1010, "用户登录失败"),
    
    /** Token无效 */
    TOKEN_INVALID(1011, "Token无效"),
    
    /** Token已过期 */
    TOKEN_EXPIRED(1012, "Token已过期"),
    
    /** 邮件发送失败 */
    EMAIL_SEND_FAILED(1013, "邮件发送失败");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}