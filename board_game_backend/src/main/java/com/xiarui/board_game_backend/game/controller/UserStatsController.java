package com.xiarui.board_game_backend.game.controller;

import com.xiarui.board_game_backend.common.entity.PageResult;
import com.xiarui.board_game_backend.common.entity.Result;
import com.xiarui.board_game_backend.game.entity.dto.UserStatsBatchRequest;
import com.xiarui.board_game_backend.game.entity.vo.UserStatsVO;
import com.xiarui.board_game_backend.game.service.UserStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户战绩接口.
 */
@RestController
@RequestMapping("/api/game/user-stats")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    /**
     * 获取当前登录用户的累计战绩。
     */
    @GetMapping("/me")
    public Result<UserStatsVO> getMyStats() {
        return Result.success(userStatsService.getCurrentUserStats());
    }

    /**
     * 查看任意指定用户的累计战绩。
     */
    @GetMapping("/{userId}")
    public Result<UserStatsVO> getUserStats(@PathVariable Long userId) {
        return Result.success(userStatsService.getUserStats(userId));
    }

    /**
     * 获取全服排行榜，支持分页。
     */
    @GetMapping("/leaderboard")
    public PageResult<UserStatsVO> getLeaderboard(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        return userStatsService.getLeaderboard(page, size);
    }

    /**
     * 获取以当前用户为中心的一段排行榜（便于查看自身位次）。
     */
    @GetMapping("/leaderboard/around-me")
    public Result<List<UserStatsVO>> getAroundMe(@RequestParam(defaultValue = "5") int radius) {
        return Result.success(userStatsService.getAroundMeLeaderboard(radius));
    }

    /**
     * 批量查询多个用户的战绩（用于房间或结算展示）。
     */
    @PostMapping("/batch")
    public Result<List<UserStatsVO>> batchStats(@Valid @RequestBody UserStatsBatchRequest request) {
        return Result.success(userStatsService.getBatchStats(request));
    }
}
