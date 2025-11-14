package com.xiarui.board_game_backend.game.service;

import com.xiarui.board_game_backend.game.entity.dto.CreateCustomRoomRequest;
import com.xiarui.board_game_backend.game.entity.dto.JoinRoomByCodeRequest;
import com.xiarui.board_game_backend.game.entity.dto.ReadyRequest;
import com.xiarui.board_game_backend.game.entity.vo.GameRoomVO;
import com.xiarui.board_game_backend.game.entity.vo.MatchJoinResponse;

/**
 * 房间/匹配服务.
 */
public interface GameRoomService {

    MatchJoinResponse joinRandomMatch();

    GameRoomVO createCustomRoom(CreateCustomRoomRequest request);

    GameRoomVO joinByCode(JoinRoomByCodeRequest request);

    GameRoomVO toggleReady(String roomId, ReadyRequest request);

    GameRoomVO startGame(String roomId);

    GameRoomVO getRoom(String roomId);

    GameRoomVO getMyRoom();
}
