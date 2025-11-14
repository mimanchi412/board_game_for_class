package com.xiarui.board_game_backend.game.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量查询战绩请求.
 */
@Data
public class UserStatsBatchRequest {

    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
}
