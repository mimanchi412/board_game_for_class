package com.xiarui.board_game_backend.game.entity.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对局列表简要信息。
 */
@Data
public class GameHistoryListItemVO {
    private Long matchId;
    private Long landlordUserId;
    private String winnerSide;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String role;
    private String result;
    private Integer scoreDelta;
    private Integer bombs;
    private Integer leftCards;
    private Boolean escaped;
}
