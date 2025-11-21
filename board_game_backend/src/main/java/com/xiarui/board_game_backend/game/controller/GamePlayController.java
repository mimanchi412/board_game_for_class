package com.xiarui.board_game_backend.game.controller;

import com.xiarui.board_game_backend.common.security.WsUserPrincipal;
import com.xiarui.board_game_backend.game.entity.dto.BidActionRequest;
import com.xiarui.board_game_backend.game.entity.dto.PlayCardRequest;
import com.xiarui.board_game_backend.game.entity.enums.GameEventType;
import com.xiarui.board_game_backend.game.entity.vo.GameEventMessage;
import com.xiarui.board_game_backend.game.entity.vo.GameSnapshotVO;
import com.xiarui.board_game_backend.game.service.GameMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 牌局内 WebSocket 指令。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GamePlayController {

    private final GameMatchService gameMatchService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 客户端重连或手动刷新时请求牌局快照，由服务器推送到用户专属队列。
     */
    @MessageMapping("/room/{roomId}/snapshot")
    public void snapshot(@DestinationVariable String roomId, Principal principal) {
        WsUserPrincipal user = requirePrincipal(principal);
        GameSnapshotVO snapshot = gameMatchService.getSnapshot(roomId, user.getUserId());
        messagingTemplate.convertAndSendToUser(user.getName(),
                "/queue/room/" + roomId + "/snapshot",
                GameEventMessage.of(GameEventType.SNAPSHOT, snapshot));
    }

    /**
     * 接收前端心跳消息，刷新该玩家在房间内的在线状态，防止误判托管。
     */
    @MessageMapping("/room/{roomId}/heartbeat")
    public void heartbeat(@DestinationVariable String roomId, Principal principal) {
        WsUserPrincipal user = requirePrincipal(principal);
        gameMatchService.handleHeartbeat(roomId, user.getUserId());
    }

    /**
     * 玩家主动投降入口，服务器将标记托管并广播惩罚信息。
     */
    @MessageMapping("/room/{roomId}/surrender")
    public void surrender(@DestinationVariable String roomId, Principal principal) {
        WsUserPrincipal user = requirePrincipal(principal);
        gameMatchService.handleSurrender(roomId, user.getUserId());
    }

    /**
     * 叫/抢地主。
     */
    @MessageMapping("/room/{roomId}/bid")
    public void bid(@DestinationVariable String roomId, BidActionRequest request, Principal principal) {
        WsUserPrincipal user = requirePrincipal(principal);
        gameMatchService.handleBid(roomId, user.getUserId(), request);
    }

    /**
     * 出牌。
     */
    @MessageMapping("/room/{roomId}/play")
    public void play(@DestinationVariable String roomId, PlayCardRequest request, Principal principal) {
        WsUserPrincipal user = requirePrincipal(principal);
        gameMatchService.handlePlay(roomId, user.getUserId(), request);
    }

    /**
     * 过牌/不出。
     */
    @MessageMapping("/room/{roomId}/pass")
    public void pass(@DestinationVariable String roomId, Principal principal) {
        WsUserPrincipal user = requirePrincipal(principal);
        gameMatchService.handlePass(roomId, user.getUserId());
    }

    private WsUserPrincipal requirePrincipal(Principal principal) {
        if (principal instanceof WsUserPrincipal wsUserPrincipal) {
            return wsUserPrincipal;
        }
        throw new IllegalStateException("WebSocket 身份异常");
    }

    @MessageExceptionHandler(Exception.class)
    public void handleWsException(Exception ex, Principal principal) {
        log.warn("WebSocket 处理异常", ex);
        if (principal instanceof WsUserPrincipal wsUserPrincipal) {
            messagingTemplate.convertAndSendToUser(wsUserPrincipal.getName(),
                    "/queue/errors",
                    GameEventMessage.of(GameEventType.ERROR, java.util.Map.of("message", ex.getMessage())));
        }
    }
}
