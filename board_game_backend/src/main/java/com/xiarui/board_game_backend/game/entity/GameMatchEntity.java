package com.xiarui.board_game_backend.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对应 game_match 表。
 */
@Data
@TableName("game_match")
public class GameMatchEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;
    private Long landlordUserId;
    private String winnerSide;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String remark;
}
