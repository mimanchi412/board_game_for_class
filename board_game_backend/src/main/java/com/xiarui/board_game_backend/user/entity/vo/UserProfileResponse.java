package com.xiarui.board_game_backend.user.entity.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料响应.
 */
@Data
public class UserProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private Integer gender;
    private String email;
    private String role;
    private LocalDateTime lastLoginTime;
    private String bio;
    private LocalDate birthday;
    private String region;
    private Integer score;
    private Integer winCount;
    private Integer loseCount;
    private Integer level;
    private String avatarSha256;
    private Integer avatarSize;
    private String avatarContentType;
}
