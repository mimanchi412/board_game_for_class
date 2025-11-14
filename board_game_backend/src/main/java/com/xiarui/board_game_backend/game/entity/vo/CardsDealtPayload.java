package com.xiarui.board_game_backend.game.entity.vo;

import com.xiarui.board_game_backend.game.entity.enums.PlayerRole;

import java.util.List;

/**
 * 发牌消息载荷。
 */
public record CardsDealtPayload(
        String roomId,
        String matchId,
        PlayerRole role,
        List<String> handCards,
        List<String> landlordCards
) {
}
