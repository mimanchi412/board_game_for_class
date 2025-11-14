package com.xiarui.board_game_backend.game.entity.enums;

/**
 * WebSocket 事件类型。
 */
public enum GameEventType {
    ROOM_STARTED,
    CARDS_DEALT,
    TURN_START,
    PLAY_CARD,
    PASS,
    AUTO_PLAY,
    HEARTBEAT_ACK,
    SURRENDER,
    SNAPSHOT,
    GAME_RESULT
}
