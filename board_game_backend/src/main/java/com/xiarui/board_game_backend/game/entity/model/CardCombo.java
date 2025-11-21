package com.xiarui.board_game_backend.game.entity.model;

import com.xiarui.board_game_backend.game.entity.enums.CardComboType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 牌型解析结果。
 */
@Getter
@RequiredArgsConstructor
public class CardCombo {
    private final CardComboType type;
    private final int mainRankWeight;
    private final int size;
    private final int groupCount;
}
