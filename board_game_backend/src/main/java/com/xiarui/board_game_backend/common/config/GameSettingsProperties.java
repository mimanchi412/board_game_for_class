package com.xiarui.board_game_backend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "game")
public class GameSettingsProperties {

    private Timeout timeout = new Timeout();
    private Penalty penalty = new Penalty();
    private Offline offline = new Offline();
    private MatchState matchState = new MatchState();

    @Data
    public static class Timeout {
        private long turnSeconds = 20;
        private long heartbeatSeconds = 30;
        private long heartbeatBufferSeconds = 10;
    }

    @Data
    public static class Penalty {
        private int surrender = 100;
        private int escape = 200;
    }

    @Data
    public static class Offline {
        private int maxTimeoutBeforeEscape = 2;
    }

    @Data
    public static class MatchState {
        private long activeTtlSeconds = 3600;
        private long settledTtlMinutes = 5;
    }
}
