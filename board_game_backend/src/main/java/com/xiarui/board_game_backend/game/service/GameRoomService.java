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

    /**
     * 加入随机匹配池。
     *
     * @return 匹配结果/房间信息
     */
    MatchJoinResponse joinRandomMatch();

    /**
     * 创建自定义房间。
     *
     * @param request 创建参数
     * @return 房间信息
     */
    GameRoomVO createCustomRoom(CreateCustomRoomRequest request);

    /**
     * 通过房间码加入。
     *
     * @param request 房间码参数
     * @return 房间信息
     */
    GameRoomVO joinByCode(JoinRoomByCodeRequest request);

    /**
     * 切换准备/取消准备。
     *
     * @param roomId 房间 ID
     * @param request 准备状态
     * @return 房间信息
     */
    GameRoomVO toggleReady(String roomId, ReadyRequest request);

    /**
     * 开始游戏。
     *
     * @param roomId 房间 ID
     * @return 房间信息
     */
    GameRoomVO startGame(String roomId);

    /**
     * 查询房间详情。
     *
     * @param roomId 房间 ID
     * @return 房间信息
     */
    GameRoomVO getRoom(String roomId);

    /**
     * 查询当前登录用户所在房间。
     *
     * @return 房间信息
     */
    GameRoomVO getMyRoom();
    
    /**
     * 离开房间。
     *
     * @param roomId 房间 ID
     * @return 操作结果
     */
    void leaveRoom(String roomId);
}
