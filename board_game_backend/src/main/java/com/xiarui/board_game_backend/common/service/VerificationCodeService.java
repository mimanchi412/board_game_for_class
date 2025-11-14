package com.xiarui.board_game_backend.common.service;

/**
 * 验证码服务接口
 */
public interface VerificationCodeService {

    /**
     * 生成验证码
     * @param key 验证码的键（通常是邮箱或手机号）
     * @return 生成的验证码
     */
    String generateCode(String key);

    /**
     * 验证验证码
     * @param key 验证码的键（通常是邮箱或手机号）
     * @param code 用户输入的验证码
     * @return 验证是否成功
     */
    boolean verifyCode(String key, String code);

    /**
     * 检查验证码是否存在
     * @param key 验证码的键（通常是邮箱或手机号）
     * @return 是否存在
     */
    boolean existsCode(String key);

    /**
     * 获取验证码剩余有效时间（秒）
     * @param key 验证码的键（通常是邮箱或手机号）
     * @return 剩余有效时间，如果不存在返回-1
     */
    long getCodeRemainingTime(String key);

    /**
     * 删除验证码
     * @param key 验证码的键（通常是邮箱或手机号）
     */
    void deleteCode(String key);
}