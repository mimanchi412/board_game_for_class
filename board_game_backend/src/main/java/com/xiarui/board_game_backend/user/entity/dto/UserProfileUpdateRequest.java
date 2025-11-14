package com.xiarui.board_game_backend.user.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户资料更新请求.
 */
@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过32个字符")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @Size(max = 160, message = "签名长度不能超过160个字符")
    private String bio;

    @PastOrPresent(message = "生日不能晚于今天")
    private LocalDate birthday;

    @Size(max = 64, message = "地区长度不能超过64个字符")
    private String region;
}
