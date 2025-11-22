package com.xiarui.board_game_backend.game.service;

import com.xiarui.board_game_backend.game.entity.dto.BidActionRequest;
import com.xiarui.board_game_backend.game.entity.dto.PlayCardRequest;
import com.xiarui.board_game_backend.game.entity.model.GameMatchState;
import com.xiarui.board_game_backend.game.entity.model.GameRoomInfo;
import com.xiarui.board_game_backend.game.entity.vo.GameSnapshotVO;

/**
 * 牌局生命周期管理。
 */
public interface GameMatchService {

    /**
     * 初始化牌局（发牌、确定首个行动玩家）。
     *
     * @param roomInfo 房间信息
     * @return 牌局状态
     */
    GameMatchState initializeMatch(GameRoomInfo roomInfo);

    /**
     * 获取指定房间的当前牌局快照。
     *
     * @param roomId 房间 ID
     * @param requesterId 请求用户 ID（用于显示自己的手牌）
     * @return 牌局快照
     */
    GameSnapshotVO getSnapshot(String roomId, Long requesterId);

    /**
     * 处理心跳，刷新在线状态。
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void handleHeartbeat(String roomId, Long userId);

    /**
     * 主动投降/托管。
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void handleSurrender(String roomId, Long userId);

    /**
     * 心跳掉线/超时处理。
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void handleOfflineTimeout(String roomId, Long userId);

    /**
     * 叫/抢地主。
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @param request 叫/抢参数
     */
    void handleBid(String roomId, Long userId, BidActionRequest request);

    /**
     * 出牌。
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @param request 出牌请求
     */
    void handlePlay(String roomId, Long userId, PlayCardRequest request);

    /**
     * 不出/过牌。
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void handlePass(String roomId, Long userId);

    /**
     * 获取当前房间的牌局状态（从 Redis）。
     *
     * @param roomId 房间 ID
     * @return 牌局状态
     */
    GameMatchState getState(String roomId);
}
