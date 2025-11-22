package com.xiarui.board_game_backend.game.task;

import com.xiarui.board_game_backend.common.config.GameSettingsProperties;
import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.game.service.GameMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 周期性清理 Redis 脏数据 / 掉线托管。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameDataCleanupTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final GameMatchService gameMatchService;
    private final GameSettingsProperties gameSettings;

    /**
     * 每分钟清理一次。
     */
    @Scheduled(fixedDelay = 60_000)
    public void clean() {
        cleanupUserBindings();
        cleanupHeartbeatsAndSurrender();
    }

    private void cleanupUserBindings() {
        String pattern = RedisConstants.GAME_ROOM_USER_PREFIX + "*";
        for (String key : scanKeys(pattern)) {
            String roomId = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(roomId)) {
                stringRedisTemplate.delete(key);
                continue;
            }
            boolean exists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstants.GAME_ROOM_INFO_PREFIX + roomId));
            if (!exists) {
                stringRedisTemplate.delete(key);
                log.info("Cleanup stale room binding key={}, roomId={}", key, roomId);
            }
        }
    }

    private void cleanupHeartbeatsAndSurrender() {
        long now = System.currentTimeMillis();
        long expireMs = (gameSettings.getTimeout().getHeartbeatSeconds() + gameSettings.getTimeout().getHeartbeatBufferSeconds()) * 1000;
        String pattern = RedisConstants.GAME_ROOM_HEARTBEAT_PREFIX + "*";
        for (String key : scanKeys(pattern)) {
            String ts = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(ts)) {
                stringRedisTemplate.delete(key);
                continue;
            }
            long last = 0L;
            try {
                last = Long.parseLong(ts);
            } catch (NumberFormatException e) {
                stringRedisTemplate.delete(key);
                continue;
            }
            if (now - last < expireMs) {
                continue;
            }
            String suffix = key.substring(RedisConstants.GAME_ROOM_HEARTBEAT_PREFIX.length());
            String[] parts = suffix.split(":");
            if (parts.length < 2) {
                stringRedisTemplate.delete(key);
                continue;
            }
            String roomId = parts[0];
            Long userId = null;
            try {
                userId = Long.valueOf(parts[1]);
            } catch (NumberFormatException e) {
                stringRedisTemplate.delete(key);
                continue;
            }
            stringRedisTemplate.delete(key);
            try {
                gameMatchService.handleOfflineTimeout(roomId, userId);
                log.info("Auto offline handling triggered, room={}, user={}", roomId, userId);
            } catch (Exception e) {
                log.debug("Auto offline handling failed for room={}, user={}, msg={}", roomId, userId, e.getMessage());
            }
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        stringRedisTemplate.execute((RedisConnection connection) -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                cursor.forEachRemaining(item -> keys.add(new String(item, StandardCharsets.UTF_8)));
            }
            return null;
        });
        return keys;
    }
}
