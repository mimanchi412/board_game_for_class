package com.xiarui.board_game_backend.game.controller;

import com.xiarui.board_game_backend.common.entity.Result;
import com.xiarui.board_game_backend.game.entity.dto.CreateCustomRoomRequest;
import com.xiarui.board_game_backend.game.entity.dto.JoinRoomByCodeRequest;
import com.xiarui.board_game_backend.game.entity.dto.ReadyRequest;
import com.xiarui.board_game_backend.game.entity.vo.GameRoomVO;
import com.xiarui.board_game_backend.game.entity.vo.MatchJoinResponse;
import com.xiarui.board_game_backend.game.service.GameRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房间/匹配相关接口.
 */
@RestController
@RequestMapping("/api/game/rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;

    /**
     * 加入随机匹配。
     */
    @PostMapping("/random/join")
    public Result<MatchJoinResponse> joinRandomMatch() {
        return Result.success(gameRoomService.joinRandomMatch());
    }

    /**
     * 创建自定义房间。
     */
    @PostMapping("/custom")
    public Result<GameRoomVO> createCustomRoom(@Valid @RequestBody CreateCustomRoomRequest request) {
        return Result.success("创建房间成功", gameRoomService.createCustomRoom(request));
    }

    /**
     * 通过房间码加入房间。
     */
    @PostMapping("/custom/join")
    public Result<GameRoomVO> joinByCode(@Valid @RequestBody JoinRoomByCodeRequest request) {
        return Result.success("加入房间成功", gameRoomService.joinByCode(request));
    }

    /**
     * 准备/取消准备。
     */
    @PostMapping("/{roomId}/ready")
    public Result<GameRoomVO> ready(@PathVariable String roomId, @Valid @RequestBody ReadyRequest request) {
        return Result.success(gameRoomService.toggleReady(roomId, request));
    }

    /**
     * 开始游戏。
     */
    @PostMapping("/{roomId}/start")
    public Result<GameRoomVO> start(@PathVariable String roomId) {
        return Result.success(gameRoomService.startGame(roomId));
    }

    /**
     * 查询自己当前所在房间。
     */
    @GetMapping("/my")
    public Result<GameRoomVO> getMyRoom() {
        return Result.success(gameRoomService.getMyRoom());
    }

    /**
     * 查询房间详情。
     */
    @GetMapping("/{roomId}")
    public Result<GameRoomVO> getRoom(@PathVariable String roomId) {
        return Result.success(gameRoomService.getRoom(roomId));
    }
    
    /**
     * 离开房间。
     */
    @PostMapping("/{roomId}/leave")
    public Result<Void> leave(@PathVariable String roomId) {
        gameRoomService.leaveRoom(roomId);
        return Result.success("离开房间成功");
    }
}
