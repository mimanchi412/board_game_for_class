package com.xiarui.board_game_backend.game.entity.vo;

import java.util.List;

/**
 * 最近一次出牌信息。
 */
public record LastPlaySnapshotVO(
        Long userId,
        List<String> cards,
        String pattern,
        long playedAtEpochMillis
) {
}
