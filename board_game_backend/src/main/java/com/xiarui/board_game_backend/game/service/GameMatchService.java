package com.xiarui.board_game_backend.game.service;

import com.xiarui.board_game_backend.game.entity.model.GameMatchState;
import com.xiarui.board_game_backend.game.entity.model.GameRoomInfo;
import com.xiarui.board_game_backend.game.entity.vo.GameSnapshotVO;

/**
 * 牌局生命周期管理。
 */
public interface GameMatchService {

    GameMatchState initializeMatch(GameRoomInfo roomInfo);

    GameSnapshotVO getSnapshot(String roomId, Long requesterId);

    void handleHeartbeat(String roomId, Long userId);

    void handleSurrender(String roomId, Long userId);

    GameMatchState getState(String roomId);
}
