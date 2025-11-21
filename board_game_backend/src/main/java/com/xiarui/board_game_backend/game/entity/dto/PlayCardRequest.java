package com.xiarui.board_game_backend.game.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 出牌请求。
 */
@Data
public class PlayCardRequest {

    /** 要出的牌列表，例如 ["S3","H3"] */
    @NotEmpty
    private List<String> cards;

    /** 牌型标识（可选），便于前后端校验描述。 */
    private String pattern;
}
