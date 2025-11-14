package com.xiarui.board_game_backend.game.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * user_stats 表对应实体（用于 MyBatis-Plus 基础操作）.
 */
@Data
@TableName("user_stats")
public class UserStatsEntity {

    @TableId
    private Long userId;
    private Integer totalGames;
    private Integer winCount;
    private Integer loseCount;
    private Integer score;
    private Integer level;
    private Integer maxStreak;
    private Integer currentStreak;
    private OffsetDateTime updateTime;
}
