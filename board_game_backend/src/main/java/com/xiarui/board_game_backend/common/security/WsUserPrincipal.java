package com.xiarui.board_game_backend.common.security;

import java.security.Principal;

/**
 * WebSocket/STOMP 会话中的认证主体，携带用户ID。
 */
public class WsUserPrincipal implements Principal {

    private final Long userId;
    private final String username;

    public WsUserPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String getName() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }
}
