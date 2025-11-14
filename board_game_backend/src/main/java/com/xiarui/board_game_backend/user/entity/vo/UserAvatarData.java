package com.xiarui.board_game_backend.user.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserAvatarData {
    private byte[] data;
    private String contentType;
    private String sha256;
}
