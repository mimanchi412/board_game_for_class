package com.xiarui.board_game_backend.game.entity.vo;

import com.xiarui.board_game_backend.game.entity.enums.GamePhase;

import java.util.List;
import java.util.Map;

/**
 * 房间内的实时快照。
 */
public record GameSnapshotVO(
        String roomId,
        String matchId,
        GamePhase phase,
        Long landlordId,
        List<Long> seatOrder,
        Map<Long, PlayerSnapshotVO> players,
        List<String> landlordCards,
        LastPlaySnapshotVO lastPlay,
        Long currentTurnUserId,
        long turnDeadlineEpochMillis,
        boolean surrendering
) {
}
