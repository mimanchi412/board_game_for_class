package com.xiarui.board_game_backend.user.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户邮箱更新请求.
 */
@Data
public class UserEmailUpdateRequest {

    @NotBlank(message = "原邮箱不能为空")
    @Email(message = "原邮箱格式不正确")
    @Size(max = 64, message = "原邮箱长度不能超过64个字符")
    private String oldEmail;

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "新邮箱格式不正确")
    @Size(max = 64, message = "新邮箱长度不能超过64个字符")
    private String newEmail;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度必须为6位")
    private String verificationCode;
}
