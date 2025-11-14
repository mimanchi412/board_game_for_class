package com.xiarui.board_game_backend.game.entity.vo;

import com.xiarui.board_game_backend.game.entity.enums.GameRoomMode;
import com.xiarui.board_game_backend.game.entity.enums.GameRoomStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 房间对外展示信息.
 */
@Data
public class GameRoomVO {
    private String roomId;
    private String roomCode;
    private GameRoomMode mode;
    private GameRoomStatus status;
    private Long ownerId;
    private List<Long> memberIds;
    private Map<Long, Boolean> readyMap;
    private Instant createdAt;
}
