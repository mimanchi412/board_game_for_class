package com.xiarui.board_game_backend.game.service;

import com.xiarui.board_game_backend.game.entity.dto.UserStatsBatchRequest;
import com.xiarui.board_game_backend.game.entity.vo.UserStatsVO;
import com.xiarui.board_game_backend.common.entity.PageResult;

import java.util.List;

/**
 * 用户战绩统计服务.
 */
public interface UserStatsService {

    UserStatsVO getCurrentUserStats();

    UserStatsVO getUserStats(Long userId);

    PageResult<UserStatsVO> getLeaderboard(long page, long size);

    List<UserStatsVO> getAroundMeLeaderboard(int radius);

    List<UserStatsVO> getBatchStats(UserStatsBatchRequest request);
}
