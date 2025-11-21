package com.xiarui.board_game_backend.game.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 叫地主动作请求。
 */
@Data
public class BidActionRequest {

    /** true 表示叫/抢地主，false 表示不叫。 */
    @NotNull
    private Boolean callLandlord;
}
