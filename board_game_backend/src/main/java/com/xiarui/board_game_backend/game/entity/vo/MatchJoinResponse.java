package com.xiarui.board_game_backend.game.entity.vo;

import lombok.Data;

/**
 * 随机匹配结果.
 */
@Data
public class MatchJoinResponse {
    private boolean matched;
    private GameRoomVO room;
    private String message;
}
