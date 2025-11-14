package com.xiarui.board_game_backend.game.entity.enums;

/**
 * 房间状态.
 */
public enum GameRoomStatus {
    WAITING,   // 人未齐
    READY,     // 人齐但未全部准备
    PLAYING    // 已开始
}
