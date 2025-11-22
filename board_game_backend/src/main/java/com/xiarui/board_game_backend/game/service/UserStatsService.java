package com.xiarui.board_game_backend.game.service;

import com.xiarui.board_game_backend.game.entity.dto.UserStatsBatchRequest;
import com.xiarui.board_game_backend.game.entity.vo.UserStatsVO;
import com.xiarui.board_game_backend.common.entity.PageResult;

import java.util.List;

/**
 * 用户战绩统计服务.
 */
public interface UserStatsService {

    /**
     * 获取当前登录用户的累计战绩。
     */
    UserStatsVO getCurrentUserStats();

    /**
     * 获取指定用户的累计战绩。
     *
     * @param userId 用户 ID
     */
    UserStatsVO getUserStats(Long userId);

    /**
     * 获取排行榜（分页）。
     *
     * @param page 页码
     * @param size 每页条数
     */
    PageResult<UserStatsVO> getLeaderboard(long page, long size);

    /**
     * 获取以当前用户为中心的一段排行榜。
     *
     * @param radius 前后显示的行数
     */
    List<UserStatsVO> getAroundMeLeaderboard(int radius);

    /**
     * 批量查询多个用户的战绩。
     *
     * @param request 用户 ID 列表请求
     */
    List<UserStatsVO> getBatchStats(UserStatsBatchRequest request);
}
