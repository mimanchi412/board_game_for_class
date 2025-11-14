package com.xiarui.board_game_backend.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiarui.board_game_backend.game.entity.UserStatsEntity;
import com.xiarui.board_game_backend.game.entity.vo.UserStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * user_stats 相关查询.
 */
@Mapper
public interface UserStatsMapper extends BaseMapper<UserStatsEntity> {

    @Select("""
        SELECT us.user_id  AS userId,
               u.username AS username,
               u.nickname AS nickname,
               us.total_games    AS totalGames,
               us.win_count      AS winCount,
               us.lose_count     AS loseCount,
               us.score          AS score,
               us.level          AS level,
               us.max_streak     AS maxStreak,
               us.current_streak AS currentStreak,
               us.update_time    AS updateTime
        FROM user_stats us
        LEFT JOIN users u ON u.id = us.user_id
        WHERE us.user_id = #{userId}
        """)
    UserStatsVO selectStatsByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT us.user_id  AS userId,
               u.username AS username,
               u.nickname AS nickname,
               us.total_games    AS totalGames,
               us.win_count      AS winCount,
               us.lose_count     AS loseCount,
               us.score          AS score,
               us.level          AS level,
               us.max_streak     AS maxStreak,
               us.current_streak AS currentStreak,
               us.update_time    AS updateTime
        FROM user_stats us
        LEFT JOIN users u ON u.id = us.user_id
        ORDER BY us.score DESC, us.user_id ASC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<UserStatsVO> selectLeaderboard(@Param("offset") long offset, @Param("limit") long limit);

    @Select("SELECT COUNT(1) FROM user_stats")
    long countAll();

    @Select("""
        SELECT 1 +
               COUNT(*)
        FROM user_stats
        WHERE score > (SELECT score FROM user_stats WHERE user_id = #{userId})
           OR (score = (SELECT score FROM user_stats WHERE user_id = #{userId}) AND user_id < #{userId})
        """)
    Long selectRankByUserId(@Param("userId") Long userId);

    @Select("""
        <script>
        SELECT us.user_id  AS userId,
               u.username AS username,
               u.nickname AS nickname,
               us.total_games    AS totalGames,
               us.win_count      AS winCount,
               us.lose_count     AS loseCount,
               us.score          AS score,
               us.level          AS level,
               us.max_streak     AS maxStreak,
               us.current_streak AS currentStreak,
               us.update_time    AS updateTime
        FROM user_stats us
        LEFT JOIN users u ON u.id = us.user_id
        WHERE us.user_id IN
        <foreach collection="userIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
        """)
    List<UserStatsVO> selectByUserIds(@Param("userIds") List<Long> userIds);
}
