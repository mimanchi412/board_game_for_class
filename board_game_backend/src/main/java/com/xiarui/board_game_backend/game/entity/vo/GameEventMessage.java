package com.xiarui.board_game_backend.game.entity.vo;

import com.xiarui.board_game_backend.game.entity.enums.GameEventType;

/**
 * WebSocket 推送消息统一结构。
 */
public record GameEventMessage(
        GameEventType type,
        Object payload,
        long timestamp
) {
    public static GameEventMessage of(GameEventType type, Object payload) {
        return new GameEventMessage(type, payload, System.currentTimeMillis());
    }
}
