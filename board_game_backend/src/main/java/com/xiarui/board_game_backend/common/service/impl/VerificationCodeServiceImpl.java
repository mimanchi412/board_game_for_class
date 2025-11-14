package com.xiarui.board_game_backend.common.service.impl;

import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.common.service.RedisService;
import com.xiarui.board_game_backend.common.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 验证码服务实现类
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Autowired
    private RedisService redisService;

    /** 验证码长度 */
    private static final int VERIFICATION_CODE_LENGTH = 6;

    /**
     * 生成验证码
     * @param key 验证码的键（通常是邮箱或手机号）
     * @return 生成的验证码
     */
    @Override
    public String generateCode(String key) {
        String code = generateRandomCode();
        String redisKey = RedisConstants.VERIFICATION_CODE_PREFIX + key;
        redisService.set(redisKey, code, RedisConstants.VERIFICATION_CODE_EXPIRE_TIME);
        return code;
    }

    /**
     * 验证验证码
     * @param key 验证码的键（通常是邮箱或手机号）
     * @param code 用户输入的验证码
     * @return 验证是否成功
     */
    @Override
    public boolean verifyCode(String key, String code) {
        String redisKey = RedisConstants.VERIFICATION_CODE_PREFIX + key;
        Object storedCode = redisService.get(redisKey);
        if (storedCode == null) {
            return false;
        }
        boolean isValid = storedCode.toString().equals(code);
        if (isValid) {
            // 验证成功后删除验证码
            redisService.del(redisKey);
        }
        return isValid;
    }

    /**
     * 检查验证码是否存在
     * @param key 验证码的键（通常是邮箱或手机号）
     * @return 是否存在
     */
    @Override
    public boolean existsCode(String key) {
        String redisKey = RedisConstants.VERIFICATION_CODE_PREFIX + key;
        return redisService.hasKey(redisKey);
    }

    /**
     * 获取验证码剩余有效时间（秒）
     * @param key 验证码的键（通常是邮箱或手机号）
     * @return 剩余有效时间，如果不存在返回-1
     */
    @Override
    public long getCodeRemainingTime(String key) {
        String redisKey = RedisConstants.VERIFICATION_CODE_PREFIX + key;
        return redisService.getExpire(redisKey);
    }

    /**
     * 删除验证码
     * @param key 验证码的键（通常是邮箱或手机号）
     */
    @Override
    public void deleteCode(String key) {
        String redisKey = RedisConstants.VERIFICATION_CODE_PREFIX + key;
        redisService.del(redisKey);
    }

    /**
     * 生成随机验证码
     * @return 验证码
     */
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}