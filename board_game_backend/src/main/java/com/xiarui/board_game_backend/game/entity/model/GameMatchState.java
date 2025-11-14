package com.xiarui.board_game_backend.game.entity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xiarui.board_game_backend.game.entity.enums.GamePhase;
import com.xiarui.board_game_backend.game.entity.enums.PlayerRole;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private LastPlay lastPlay;
    private Long currentTurnUserId;
    private long turnDeadlineEpochMillis;
    private boolean surrendering;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

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
}
