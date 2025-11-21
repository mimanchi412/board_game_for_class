package com.xiarui.board_game_backend.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiarui.board_game_backend.game.entity.GameMatchEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * game_match 基础操作。
 */
@Mapper
public interface GameMatchMapper extends BaseMapper<GameMatchEntity> {
}
