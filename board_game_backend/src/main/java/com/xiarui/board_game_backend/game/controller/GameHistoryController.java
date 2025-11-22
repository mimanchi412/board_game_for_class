package com.xiarui.board_game_backend.game.controller;

import com.xiarui.board_game_backend.common.entity.PageResult;
import com.xiarui.board_game_backend.common.entity.Result;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryDetailVO;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryListItemVO;
import com.xiarui.board_game_backend.game.service.GameHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对局历史与详情接口。
 */
@RestController
@RequestMapping("/api/game/history")
@RequiredArgsConstructor
public class GameHistoryController {

    private final GameHistoryService gameHistoryService;

    /**
     * 获取当前登录用户的历史对局列表（分页）。
     */
    @GetMapping("/my")
    public PageResult<GameHistoryListItemVO> listMyHistory(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "20") long size) {
        return gameHistoryService.listMyHistory(page, size);
    }

    /**
     * 查询指定对局的详情（玩家结果 + 出牌流水）。
     */
    @GetMapping("/{matchId}")
    public Result<GameHistoryDetailVO> getDetail(@PathVariable Long matchId) {
        return Result.success(gameHistoryService.getMatchDetail(matchId));
    }
}
