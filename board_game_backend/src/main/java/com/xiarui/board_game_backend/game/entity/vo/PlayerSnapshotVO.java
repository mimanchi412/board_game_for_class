package com.xiarui.board_game_backend.game.entity.vo;

import com.xiarui.board_game_backend.game.entity.enums.PlayerRole;

import java.util.List;

/**
 * 单个玩家在快照中的视图。
 */
public record PlayerSnapshotVO(
        Long userId,
        PlayerRole role,
        int cardCount,
        List<String> handCards,
        boolean autoPlay,
        boolean surrendered,
        int scoreDelta
) {
}
