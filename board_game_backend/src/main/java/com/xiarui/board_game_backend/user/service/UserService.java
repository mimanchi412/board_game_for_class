package com.xiarui.board_game_backend.user.service;

import com.xiarui.board_game_backend.user.entity.dto.UserEmailUpdateRequest;
import com.xiarui.board_game_backend.user.entity.dto.UserProfileUpdateRequest;
import com.xiarui.board_game_backend.user.entity.vo.UserAvatarData;
import com.xiarui.board_game_backend.user.entity.vo.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * 用户领域服务，封装资料查询、更新、邮箱修改与头像处理等能力。
 */
public interface UserService {

    /**
     * 获取当前登录用户的个人资料。
     */
    UserProfileResponse getCurrentUserProfile();

    /**
     * 更新当前用户的基础资料（昵称、性别、签名等）。
     *
     * @param request 资料更新请求
     * @return 最新的个人资料
     */
    UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request);

    /**
     * 更新绑定邮箱，要求提供旧邮箱以及验证码。
     *
     * @param request 邮箱更新请求
     */
    void updateEmail(UserEmailUpdateRequest request);

    /**
     * 上传/覆盖用户头像。
     *
     * @param file 头像文件
     */
    void uploadAvatar(MultipartFile file);

    /**
     * 获取头像二进制数据及相关元信息。
     *
     * @return 头像数据，可为空
     */
    Optional<UserAvatarData> getAvatar();

    /**
     * 根据用户ID获取用户信息。
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserProfileResponse getUserById(Long userId);
}
