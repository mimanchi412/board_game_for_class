package com.xiarui.board_game_backend.common.interceptor;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.auth.uitils.JwtUtil;
import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.common.security.WsUserPrincipal;
import com.xiarui.board_game_backend.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

/**
 * STOMP 鉴权及房间订阅校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserAccountMapper userAccountMapper;
    private final RedisService redisService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE, SEND -> handleDestinationCheck(accessor);
            default -> {
            }
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("缺少认证信息");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.validateToken(token) || !redisService.hasKey(RedisConstants.USER_TOKEN_PREFIX + token)) {
            throw new IllegalArgumentException("无效或过期的凭证");
        }
        String username = jwtUtil.getUsernameFromToken(token);
        UserAccount userAccount = userAccountMapper.findByUsername(username);
        if (userAccount == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        accessor.setUser(new WsUserPrincipal(userAccount.getId(), userAccount.getUsername()));
    }

    private void handleDestinationCheck(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new IllegalArgumentException("未登录无法操作");
        }
        Optional<String> roomIdOpt = resolveRoomId(accessor.getDestination());
        roomIdOpt.ifPresent(roomId -> ensureRoomMembership(principal, roomId));
    }

    private void ensureRoomMembership(Principal principal, String roomId) {
        if (!(principal instanceof WsUserPrincipal wsUserPrincipal)) {
            throw new IllegalArgumentException("身份信息异常");
        }
        String boundRoom = stringRedisTemplate.opsForValue()
                .get(RedisConstants.GAME_ROOM_USER_PREFIX + wsUserPrincipal.getUserId());
        if (!StringUtils.hasText(boundRoom) || !roomId.equals(boundRoom)) {
            throw new IllegalArgumentException("无权访问该房间");
        }
    }

    private Optional<String> resolveRoomId(String destination) {
        if (!StringUtils.hasText(destination)) {
            return Optional.empty();
        }
        String[] segments = destination.split("/");
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (("rooms".equals(segment) || "room".equals(segment)) && i + 1 < segments.length) {
                return Optional.ofNullable(segments[i + 1]).filter(StringUtils::hasText);
            }
        }
        return Optional.empty();
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = accessor.getFirstNativeHeader("token");
        }
        if (!StringUtils.hasText(token)) {
            token = accessor.getFirstNativeHeader("access_token");
        }
        if (StringUtils.hasText(token)) {
            return token;
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object attr = sessionAttributes.get(WebSocketAuthHandshakeInterceptor.TOKEN_ATTRIBUTE);
            if (attr instanceof String str && StringUtils.hasText(str)) {
                return str;
            }
        }
        return null;
    }
}
