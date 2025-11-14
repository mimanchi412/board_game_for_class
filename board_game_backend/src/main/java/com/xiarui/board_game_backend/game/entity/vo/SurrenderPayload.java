package com.xiarui.board_game_backend.game.entity.vo;

/**
 * 投降事件载荷。
 */
public record SurrenderPayload(
        String roomId,
        String matchId,
        Long userId,
        int penaltyScore
) {
}
