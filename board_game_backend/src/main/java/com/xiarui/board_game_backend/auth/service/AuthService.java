package com.xiarui.board_game_backend.auth.service;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.entity.dto.LoginRequest;
import com.xiarui.board_game_backend.auth.entity.dto.RegisterRequest;
import com.xiarui.board_game_backend.auth.entity.dto.ResetPasswordRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册成功的用户信息
     */
    UserAccount register(RegisterRequest registerRequest);

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return JWT令牌
     */
    String login(LoginRequest loginRequest);

    /**
     * 重置密码
     *
     * @param resetPasswordRequest 重置密码请求
     * @return 重置是否成功
     */
    boolean resetPassword(ResetPasswordRequest resetPasswordRequest);

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱地址
     * @return 发送是否成功
     */
    boolean sendVerificationCode(String email);

    /**
     * 验证邮箱验证码
     *
     * @param email 邮箱地址
     * @param code 验证码
     * @return 验证是否成功
     */
    boolean verifyCode(String email, String code);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserAccount getUserByUsername(String username);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 存在返回true，否则返回false
     */
    boolean existsByEmail(String email);

    /**
     * 退出登录
     *
     * @param authorizationHeader Authorization头部
     */
    void logout(String authorizationHeader);
}
