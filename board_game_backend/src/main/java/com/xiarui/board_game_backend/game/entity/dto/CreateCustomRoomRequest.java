package com.xiarui.board_game_backend.game.entity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建自定义房间请求.
 */
@Data
public class CreateCustomRoomRequest {

    @Size(max = 32, message = "房间名称长度不能超过32字符")
    private String roomName;
}
