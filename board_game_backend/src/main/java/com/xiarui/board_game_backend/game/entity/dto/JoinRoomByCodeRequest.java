package com.xiarui.board_game_backend.game.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通过房间码加入房间请求.
 */
@Data
public class JoinRoomByCodeRequest {

    @NotBlank(message = "房间码不能为空")
    @Size(min = 6, max = 6, message = "房间码必须为6位")
    private String roomCode;
}
