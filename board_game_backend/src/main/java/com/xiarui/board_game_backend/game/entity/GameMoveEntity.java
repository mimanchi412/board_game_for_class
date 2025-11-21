package com.xiarui.board_game_backend.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import com.xiarui.board_game_backend.common.handler.StringListJsonbTypeHandler;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 对应 game_move 表。
 */
@Data
@TableName(value = "game_move", autoResultMap = true)
public class GameMoveEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchId;
    private Integer stepNo;
    private Long playerId;
    private Integer seat;
    private String pattern;
    @TableField(value = "cards_json", typeHandler = StringListJsonbTypeHandler.class, jdbcType = JdbcType.OTHER)
    private List<String> cards;
    private Boolean beatsPrev;
    private OffsetDateTime createdAt;
}
