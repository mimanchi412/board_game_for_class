package com.xiarui.board_game_backend.game.entity.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户累计战绩响应对象.
 */
@Data
public class UserStatsVO {

    private Long userId;
    private String username;
    private String nickname;
    private Integer totalGames;
    private Integer winCount;
    private Integer loseCount;
    private Integer score;
    private Integer level;
    private Integer maxStreak;
    private Integer currentStreak;
    private OffsetDateTime updateTime;
    private Long rank;
}
