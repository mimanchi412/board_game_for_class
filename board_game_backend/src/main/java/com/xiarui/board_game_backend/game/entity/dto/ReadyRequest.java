package com.xiarui.board_game_backend.game.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 准备/取消准备请求.
 */
@Data
public class ReadyRequest {

    @NotNull
    private Boolean ready;
}
