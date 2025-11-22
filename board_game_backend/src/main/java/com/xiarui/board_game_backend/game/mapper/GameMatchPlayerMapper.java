package com.xiarui.board_game_backend.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiarui.board_game_backend.game.entity.GameMatchPlayerEntity;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * game_match_player 基础操作。
 */
@Mapper
public interface GameMatchPlayerMapper extends BaseMapper<GameMatchPlayerEntity> {

    @Select("""
        SELECT gmp.match_id       AS matchId,
               gm.landlord_user_id AS landlordUserId,
               gm.winner_side     AS winnerSide,
               gm.start_time      AS startTime,
               gm.end_time        AS endTime,
               gmp.role           AS role,
               gmp.result         AS result,
               gmp.score_delta    AS scoreDelta,
               gmp.bombs          AS bombs,
               gmp.left_cards     AS leftCards,
               gmp.escaped        AS escaped
        FROM game_match_player gmp
        JOIN game_match gm ON gm.id = gmp.match_id
        WHERE gmp.user_id = #{userId}
        ORDER BY gm.start_time DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<GameHistoryListItemVO> selectHistoryByUser(@Param("userId") Long userId,
                                                    @Param("offset") long offset,
                                                    @Param("limit") long limit);

    @Select("SELECT COUNT(1) FROM game_match_player WHERE user_id = #{userId}")
    long countHistoryByUser(@Param("userId") Long userId);
}
