package com.xiarui.board_game_backend.auth.controller;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.entity.dto.LoginRequest;
import com.xiarui.board_game_backend.auth.entity.dto.RegisterRequest;
import com.xiarui.board_game_backend.auth.entity.dto.ResetPasswordRequest;
import com.xiarui.board_game_backend.auth.entity.dto.SendCodeRequest;
import com.xiarui.board_game_backend.auth.service.AuthService;
import com.xiarui.board_game_backend.common.entity.Result;
import com.xiarui.board_game_backend.common.entity.ResultCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证相关接口.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册.
     */
    @PostMapping("/register")
    public Result<UserAccount> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserAccount userAccount = authService.register(registerRequest);
        userAccount.setPassword(null);
        return Result.success("注册成功", userAccount);
    }

    /**
     * 用户登录.
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        return Result.success("登录成功", data);
    }

    /**
     * 重置密码.
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        boolean success = authService.resetPassword(resetPasswordRequest);
        return success ? Result.success("密码重置成功") : Result.error(ResultCode.PASSWORD_RESET_FAILED);
    }

    /**
     * 发送邮箱验证码.
     */
    @PostMapping("/send-code")
    public Result<Void> sendVerificationCode(@Valid @RequestBody SendCodeRequest sendCodeRequest) {
        authService.sendVerificationCode(sendCodeRequest.getEmail());
        return Result.success("验证码发送成功");
    }

    /**
     * 退出登录.
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success("退出成功");
    }
}
