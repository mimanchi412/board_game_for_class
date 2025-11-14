package com.xiarui.board_game_backend.auth.service.impl;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.entity.dto.LoginRequest;
import com.xiarui.board_game_backend.auth.entity.dto.RegisterRequest;
import com.xiarui.board_game_backend.auth.entity.dto.ResetPasswordRequest;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.auth.service.AuthService;
import com.xiarui.board_game_backend.auth.uitils.JwtUtil;
import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.common.service.MailService;
import com.xiarui.board_game_backend.common.service.RedisService;
import com.xiarui.board_game_backend.common.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.OffsetDateTime;

/**
 * 认证服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationCodeService verificationCodeService;
    private final MailService mailService;
    private final RedisService redisService;
    private byte[] defaultAvatarBytes;
    private String defaultAvatarSha256;
    private Integer defaultAvatarSize;
    private final String defaultAvatarContentType = "image/png";

    @PostConstruct
    private void initDefaultAvatar() {
        ClassPathResource resource = new ClassPathResource("templates/initial_avatar.png");
        if (!resource.exists()) {
            log.warn("默认头像 initial_avatar.png 不存在，将跳过默认头像设置");
            return;
        }
        try (var inputStream = resource.getInputStream()) {
            defaultAvatarBytes = inputStream.readAllBytes();
            defaultAvatarSize = defaultAvatarBytes.length;
            defaultAvatarSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(defaultAvatarBytes));
            log.info("默认头像加载完成，大小 {} bytes", defaultAvatarSize);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("加载默认头像失败", e);
            defaultAvatarBytes = null;
        }
    }

    @Override
    @Transactional
    public UserAccount register(RegisterRequest registerRequest) {
        if (existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        validateVerificationCode(registerRequest.getEmail(), registerRequest.getVerificationCode());

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(registerRequest.getUsername());
        userAccount.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userAccount.setNickname(registerRequest.getNickname());
        userAccount.setGender(registerRequest.getGender());
        userAccount.setEmail(registerRequest.getEmail());
        userAccount.setRole("USER");
        applyDefaultAvatar(userAccount);

        userAccountMapper.insert(userAccount);
        return userAccount;
    }

    @Override
    public String login(LoginRequest loginRequest) {
        UserAccount userAccount = userAccountMapper.findByUsername(loginRequest.getUsername());
        if (userAccount == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), userAccount.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(userAccount.getUsername(), userAccount.getRole());
        userAccount.setLastLoginTime(OffsetDateTime.now());
        userAccountMapper.updateById(userAccount);
        redisService.set(
                RedisConstants.USER_TOKEN_PREFIX + token,
                userAccount.getUsername(),
                RedisConstants.USER_TOKEN_EXPIRE_TIME
        );
        return token;
    }

    @Override
    @Transactional
    public boolean resetPassword(ResetPasswordRequest resetPasswordRequest) {
        UserAccount userAccount = userAccountMapper.findByUsernameOrEmail(resetPasswordRequest.getUsernameOrEmail());
        if (userAccount == null) {
            throw new RuntimeException("用户不存在");
        }

        String email = userAccount.getEmail();
        validateVerificationCode(email, resetPasswordRequest.getVerificationCode());

        if (passwordEncoder.matches(resetPasswordRequest.getNewPassword(), userAccount.getPassword())) {
            throw new RuntimeException("新密码不能与旧密码一致");
        }

        userAccount.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        return userAccountMapper.updateById(userAccount) > 0;
    }

    @Override
    public boolean sendVerificationCode(String email) {
        String rateLimitKey = getEmailRateLimitKey(email);
        if (redisService.hasKey(rateLimitKey)) {
            throw new RuntimeException("验证码请求过于频繁，请稍后再试");
        }
        String code = verificationCodeService.generateCode(email);
        try {
            mailService.sendVerificationCodeMail(email, code);
            log.info("验证码邮件任务已发送到队列, email={}", email);
        } catch (Exception ex) {
            log.error("发送验证码邮件失败, email={}", email, ex);
            throw new RuntimeException("发送验证码失败，请稍后再试");
        }
        redisService.set(rateLimitKey, 1, RedisConstants.EMAIL_CODE_RATE_LIMIT_SECONDS);
        return true;
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return verificationCodeService.verifyCode(email, code);
    }

    @Override
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null || token.isBlank()) {
            return;
        }
        redisService.del(RedisConstants.USER_TOKEN_PREFIX + token);
    }

    private void validateVerificationCode(String email, String code) {
        if (!verificationCodeService.existsCode(email)) {
            throw new RuntimeException("请先获取验证码");
        }
        long remaining = verificationCodeService.getCodeRemainingTime(email);
        if (remaining <= 0) {
            verificationCodeService.deleteCode(email);
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!verificationCodeService.verifyCode(email, code)) {
            throw new RuntimeException("验证码不正确");
        }
        redisService.del(getEmailRateLimitKey(email));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader;
    }

    @Override
    public UserAccount getUserByUsername(String username) {
        return userAccountMapper.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userAccountMapper.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userAccountMapper.existsByEmail(email);
    }

    private void applyDefaultAvatar(UserAccount userAccount) {
        if (defaultAvatarBytes == null) {
            return;
        }
        userAccount.setAvatar(defaultAvatarBytes);
        userAccount.setAvatarContentType(defaultAvatarContentType);
        userAccount.setAvatarSize(defaultAvatarSize);
        userAccount.setAvatarSha256(defaultAvatarSha256);
    }

    private String getEmailRateLimitKey(String email) {
        return RedisConstants.EMAIL_CODE_RATE_LIMIT_PREFIX + email;
    }
}
