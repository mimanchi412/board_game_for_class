package com.xiarui.board_game_backend.game.entity.model;

import com.xiarui.board_game_backend.game.entity.enums.GameRoomMode;
import com.xiarui.board_game_backend.game.entity.enums.GameRoomStatus;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 中房间信息的结构.
 */
@Data
public class GameRoomInfo {
    private String roomId;
    private String roomCode;
    private GameRoomMode mode;
    private GameRoomStatus status;
    private Long ownerId;
    private List<Long> memberIds = new ArrayList<>();
    private Map<Long, Boolean> readyMap = new HashMap<>();
    private Instant createdAt = Instant.now();
}
