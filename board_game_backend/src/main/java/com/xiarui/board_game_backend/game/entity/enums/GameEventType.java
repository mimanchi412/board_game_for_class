package com.xiarui.board_game_backend.game.entity.enums;

/**
 * WebSocket 事件类型。
 */
public enum GameEventType {
    ROOM_STARTED,
    CARDS_DEALT,
    BID_RESULT,
    TURN_START,
    PLAY_CARD,
    PASS,
    AUTO_PLAY,
    HEARTBEAT_ACK,
    SURRENDER,
    ERROR,
    SNAPSHOT,
    GAME_RESULT
}
