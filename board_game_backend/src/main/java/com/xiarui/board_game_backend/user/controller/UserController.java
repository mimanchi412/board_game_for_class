package com.xiarui.board_game_backend.user.controller;

import com.xiarui.board_game_backend.common.entity.Result;
import com.xiarui.board_game_backend.user.entity.dto.UserEmailUpdateRequest;
import com.xiarui.board_game_backend.user.entity.dto.UserProfileUpdateRequest;
import com.xiarui.board_game_backend.user.entity.vo.UserAvatarData;
import com.xiarui.board_game_backend.user.entity.vo.UserProfileResponse;
import com.xiarui.board_game_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的个人资料。
     */
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        return Result.success(userService.getCurrentUserProfile());
    }

    /**
     * 更新昵称、性别、签名等基础资料。
     */
    @PutMapping("/profile")
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return Result.success("更新成功", userService.updateCurrentUserProfile(request));
    }

    /**
     * 更新绑定邮箱，需要携带旧邮箱及验证码。
     */
    @PutMapping("/email")
    public Result<Void> updateEmail(@Valid @RequestBody UserEmailUpdateRequest request) {
        userService.updateEmail(request);
        return Result.success("邮箱更新成功");
    }

    /**
     * 上传新的用户头像。
     */
    @PostMapping("/avatar")
    public Result<Void> uploadAvatar(@RequestParam("file") MultipartFile file) {
        userService.uploadAvatar(file);
        return Result.success("头像上传成功");
    }

    /**
     * 获取头像文件，若不存在返回404。
     */
    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar() {
        return userService.getAvatar()
                .map(this::buildAvatarResponse)
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> buildAvatarResponse(UserAvatarData avatarData) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (avatarData.getContentType() != null) {
            mediaType = MediaType.parseMediaType(avatarData.getContentType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.ETAG, avatarData.getSha256() != null ? avatarData.getSha256() : "")
                .body(avatarData.getData());
    }
}
