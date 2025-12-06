package com.xiarui.board_game_backend.user.service.impl;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.common.service.VerificationCodeService;
import com.xiarui.board_game_backend.user.entity.dto.UserEmailUpdateRequest;
import com.xiarui.board_game_backend.user.entity.dto.UserProfileUpdateRequest;
import com.xiarui.board_game_backend.user.entity.vo.UserAvatarData;
import com.xiarui.board_game_backend.user.entity.vo.UserProfileResponse;
import com.xiarui.board_game_backend.user.mapper.UserProfileMapper;
import com.xiarui.board_game_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * 用户服务实现：处理资料更新、邮箱修改、头像管理等业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final UserProfileMapper userProfileMapper;
    private final VerificationCodeService verificationCodeService;

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        return convertToResponse(getCurrentUserAccount());
    }

    /**
     * 资料更新需要事务保证原子性（避免部分字段更新成功）。
     */
    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request) {
        UserAccount userAccount = getCurrentUserAccount();
        userAccount.setNickname(request.getNickname());
        userAccount.setGender(request.getGender());
        userAccount.setBio(request.getBio());
        userAccount.setBirthday(request.getBirthday());
        userAccount.setRegion(request.getRegion());
        userProfileMapper.updateById(userAccount);
        return convertToResponse(userAccount);
    }

    /**
     * 邮箱更新前会验证旧邮箱+验证码，并校验新邮箱唯一性。
     */
    @Override
    @Transactional
    public void updateEmail(UserEmailUpdateRequest request) {
        UserAccount userAccount = getCurrentUserAccount();
        if (!userAccount.getEmail().equalsIgnoreCase(request.getOldEmail())) {
            throw new RuntimeException("原邮箱不匹配");
        }
        validateVerificationCode(request.getOldEmail(), request.getVerificationCode());
        if (userProfileMapper.existsByEmailExcludingId(request.getNewEmail(), userAccount.getId())) {
            throw new RuntimeException("新邮箱已被使用");
        }
        userAccount.setEmail(request.getNewEmail());
        userProfileMapper.updateById(userAccount);
    }

    /**
     * 头像上传会校验大小及格式，并记录指纹信息。
     */
    @Override
    @Transactional
    public void uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传有效的头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("头像大小不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new RuntimeException("仅支持PNG/JPEG/WebP格式的图片");
        }
        try {
            byte[] data = file.getBytes();
            UserAccount userAccount = getCurrentUserAccount();
            userAccount.setAvatar(data);
            userAccount.setAvatarContentType(contentType);
            userAccount.setAvatarSize((int) file.getSize());
            userAccount.setAvatarSha256(sha256Hex(data));
            userProfileMapper.updateById(userAccount);
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.error("上传头像失败", ex);
            throw new RuntimeException("上传头像失败，请稍后再试");
        }
    }

    @Override
    public Optional<UserAvatarData> getAvatar() {
        UserAccount userAccount = getCurrentUserAccount();
        if (userAccount.getAvatar() == null) {
            return Optional.empty();
        }
        return Optional.of(new UserAvatarData(
                userAccount.getAvatar(),
                userAccount.getAvatarContentType(),
                userAccount.getAvatarSha256()
        ));
    }

    /**
     * 获取当前认证用户的完整实体。
     */
    private UserAccount getCurrentUserAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("未登录");
        }
        UserAccount userAccount = userProfileMapper.findByUsername(authentication.getName());
        if (userAccount == null) {
            throw new RuntimeException("用户不存在");
        }
        return userAccount;
    }

    /**
     * 验证邮箱验证码是否存在、未过期且匹配。
     */
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
    }

    /**
     * 将实体转换为对外响应对象。
     */
    private UserProfileResponse convertToResponse(UserAccount userAccount) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(userAccount.getId());
        response.setUsername(userAccount.getUsername());
        response.setNickname(userAccount.getNickname());
        response.setGender(userAccount.getGender());
        response.setEmail(userAccount.getEmail());
        response.setRole(userAccount.getRole());
        if (userAccount.getLastLoginTime() != null) {
            response.setLastLoginTime(userAccount.getLastLoginTime().toLocalDateTime());
        }
        response.setBio(userAccount.getBio());
        response.setBirthday(userAccount.getBirthday());
        response.setRegion(userAccount.getRegion());
        response.setScore(userAccount.getScore());
        response.setWinCount(userAccount.getWinCount());
        response.setLoseCount(userAccount.getLoseCount());
        response.setLevel(userAccount.getLevel());
        response.setAvatarSha256(userAccount.getAvatarSha256());
        response.setAvatarSize(userAccount.getAvatarSize());
        response.setAvatarContentType(userAccount.getAvatarContentType());
        return response;
    }

    private String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }

    @Override
    public UserProfileResponse getUserById(Long userId) {
        UserAccount userAccount = userProfileMapper.selectById(userId);
        if (userAccount == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToResponse(userAccount);
    }
}
