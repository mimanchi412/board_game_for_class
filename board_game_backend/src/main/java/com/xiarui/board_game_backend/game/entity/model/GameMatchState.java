package com.xiarui.board_game_backend.game.entity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xiarui.board_game_backend.game.entity.enums.GamePhase;
import com.xiarui.board_game_backend.game.entity.enums.PlayerRole;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Redis 中维护的牌局状态。
 */
@Data
public class GameMatchState {

    private String matchId;
    private String roomId;
    private GamePhase phase = GamePhase.WAITING;
    private Long landlordId;
    private List<Long> seatOrder = new ArrayList<>();
    private List<PlayerState> players = new ArrayList<>();
    private List<String> landlordCards = new ArrayList<>();
    private Map<Long, Boolean> bidMap = new HashMap<>();
    private Map<Long, Boolean> robMap = new HashMap<>();
    private Long initialCallerId;
    private Long highestBidderId;
    private int remainingBidTurns;
    private int bidMultiplier = 1;
    private int consecutiveNoRob;
    private LastPlay lastPlay;
    private Long currentTurnUserId;
    private long turnDeadlineEpochMillis;
    private boolean surrendering;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    /**
     * 对局开始/结束时间（持久化时计算时长使用）。
     */
    private Instant startTime = Instant.now();
    private Instant endTime;
    /**
     * 出牌流水，结算时写入 game_move。
     */
    private List<MoveRecord> moves = new ArrayList<>();
    private int nextStepNo = 1;

    @JsonIgnore
    public PlayerState findPlayer(Long userId) {
        return players.stream().filter(p -> p.getUserId().equals(userId)).findFirst().orElse(null);
    }

    @Data
    public static class PlayerState {
        private Long userId;
        private PlayerRole role = PlayerRole.UNKNOWN;
        private List<String> handCards = new ArrayList<>();
        private boolean autoPlay;
        private boolean surrendered;
        private int scoreDelta;
    }

    @Data
    public static class LastPlay {
        private Long userId;
        private List<String> cards;
        private String pattern;
        private long playedAtEpochMillis;
    }

    @Data
    public static class MoveRecord {
        private int stepNo;
        private Long userId;
        private String pattern;
        private List<String> cards = new ArrayList<>();
        private boolean beatsPrev;
        private Instant createdAt = Instant.now();

        @JsonIgnore
        public boolean isPass() {
            return cards == null || cards.isEmpty() || Objects.equals(pattern, "PASS");
        }
    }
}
