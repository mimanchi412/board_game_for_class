package com.xiarui.board_game_backend.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对应 game_match_player 表。
 */
@Data
@TableName("game_match_player")
public class GameMatchPlayerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchId;
    private Long userId;
    private Integer seat;
    private String role;
    private String result;
    private Integer scoreDelta;
    private Integer bombs;
    private Integer leftCards;
    private Integer durationSec;
}
