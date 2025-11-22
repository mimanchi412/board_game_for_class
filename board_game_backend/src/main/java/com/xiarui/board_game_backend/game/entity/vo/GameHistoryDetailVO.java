package com.xiarui.board_game_backend.game.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameHistoryDetailVO {
    private Long matchId;
    private Long landlordUserId;
    private String winnerSide;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String remark;
    private List<PlayerResult> players;
    private List<MoveRecord> moves;

    @Data
    public static class PlayerResult {
        private Long userId;
        private Integer seat;
        private String role;
        private String result;
        private Integer scoreDelta;
        private Integer bombs;
        private Integer leftCards;
        private Integer durationSec;
        private Boolean escaped;
    }

    @Data
    public static class MoveRecord {
        private Integer stepNo;
        private Long playerId;
        private Integer seat;
        private String pattern;
        private List<String> cards;
        private Boolean beatsPrev;
        private OffsetDateTime createdAt;
    }
}
