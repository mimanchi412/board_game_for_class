package com.xiarui.board_game_backend.game.service;

import com.xiarui.board_game_backend.common.entity.PageResult;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryDetailVO;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryListItemVO;

public interface GameHistoryService {

    /**
        * 分页获取当前登录用户的历史对局列表。
        *
        * @param page 页码（从 1 开始）
        * @param size 每页条数
        * @return 对局列表分页结果
        */
    PageResult<GameHistoryListItemVO> listMyHistory(long page, long size);

    /**
     * 根据 matchId 查询对局详情（玩家结果 + 出牌流水）。
     *
     * @param matchId 对局 ID
     * @return 对局详情
     */
    GameHistoryDetailVO getMatchDetail(Long matchId);
}
