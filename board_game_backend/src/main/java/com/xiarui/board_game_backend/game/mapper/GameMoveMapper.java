package com.xiarui.board_game_backend.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiarui.board_game_backend.game.entity.GameMoveEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * game_move 基础操作。
 */
@Mapper
public interface GameMoveMapper extends BaseMapper<GameMoveEntity> {
}
