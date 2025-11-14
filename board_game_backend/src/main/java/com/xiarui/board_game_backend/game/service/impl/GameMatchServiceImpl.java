package com.xiarui.board_game_backend.game.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.game.entity.enums.GameEventType;
import com.xiarui.board_game_backend.game.entity.enums.GamePhase;
import com.xiarui.board_game_backend.game.entity.model.GameMatchState;
import com.xiarui.board_game_backend.game.entity.model.GameRoomInfo;
import com.xiarui.board_game_backend.game.entity.vo.CardsDealtPayload;
import com.xiarui.board_game_backend.game.entity.vo.GameEventMessage;
import com.xiarui.board_game_backend.game.entity.vo.GameSnapshotVO;
import com.xiarui.board_game_backend.game.entity.vo.LastPlaySnapshotVO;
import com.xiarui.board_game_backend.game.entity.vo.PlayerSnapshotVO;
import com.xiarui.board_game_backend.game.entity.vo.SurrenderPayload;
import com.xiarui.board_game_backend.game.service.GameMatchService;
import com.xiarui.board_game_backend.game.util.DeckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 牌局管理实现，负责发牌、快照、心跳等基础能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameMatchServiceImpl implements GameMatchService {

    private static final String USER_QUEUE_PREFIX = "/queue/room/";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserAccountMapper userAccountMapper;

    private final ConcurrentHashMap<Long, String> usernameCache = new ConcurrentHashMap<>();

    @Override
    public GameMatchState initializeMatch(GameRoomInfo roomInfo) {
        GameMatchState state = new GameMatchState();
        state.setRoomId(roomInfo.getRoomId());
        state.setMatchId(UUID.randomUUID().toString());
        state.setPhase(GamePhase.BID);
        state.setSeatOrder(new ArrayList<>(roomInfo.getMemberIds()));
        state.setCurrentTurnUserId(roomInfo.getMemberIds().get(0));
        state.setTurnDeadlineEpochMillis(Instant.now()
                .plusSeconds(RedisConstants.GAME_TURN_TIMEOUT_SECONDS)
                .toEpochMilli());

        List<String> deck = DeckUtil.shuffledDeck();
        for (int i = 0; i < roomInfo.getMemberIds().size(); i++) {
            Long userId = roomInfo.getMemberIds().get(i);
            GameMatchState.PlayerState playerState = new GameMatchState.PlayerState();
            playerState.setUserId(userId);
            int fromIndex = i * 17;
            int toIndex = Math.min(fromIndex + 17, deck.size());
            List<String> handCards = new ArrayList<>(deck.subList(fromIndex, toIndex));
            DeckUtil.sortCards(handCards);
            playerState.setHandCards(handCards);
            state.getPlayers().add(playerState);
        }
        state.setLandlordCards(new ArrayList<>(deck.subList(51, 54)));
        persistState(state);
        broadcastRoomStarted(state);
        pushHands(state);
        return state;
    }

    @Override
    public GameSnapshotVO getSnapshot(String roomId, Long requesterId) {
        GameMatchState state = getState(roomId);
        Map<Long, PlayerSnapshotVO> snapshotMap = state.getPlayers().stream()
                .collect(Collectors.toMap(
                        GameMatchState.PlayerState::getUserId,
                        playerState -> toPlayerSnapshot(playerState, requesterId),
                        (a, b) -> a,
                        LinkedHashMap::new));
        LastPlaySnapshotVO lastPlaySnapshotVO = null;
        if (state.getLastPlay() != null && state.getLastPlay().getCards() != null) {
            lastPlaySnapshotVO = new LastPlaySnapshotVO(
                    state.getLastPlay().getUserId(),
                    state.getLastPlay().getCards(),
                    state.getLastPlay().getPattern(),
                    state.getLastPlay().getPlayedAtEpochMillis()
            );
        }
        return new GameSnapshotVO(
                state.getRoomId(),
                state.getMatchId(),
                state.getPhase(),
                state.getLandlordId(),
                state.getSeatOrder(),
                snapshotMap,
                state.getLandlordCards(),
                lastPlaySnapshotVO,
                state.getCurrentTurnUserId(),
                state.getTurnDeadlineEpochMillis(),
                state.isSurrendering()
        );
    }

    @Override
    public void handleHeartbeat(String roomId, Long userId) {
        String key = RedisConstants.GAME_ROOM_HEARTBEAT_PREFIX + roomId + ":" + userId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(RedisConstants.GAME_HEARTBEAT_TTL_SECONDS));
        String username = resolveUsername(userId);
        GameEventMessage ack = GameEventMessage.of(GameEventType.HEARTBEAT_ACK,
                Map.of("roomId", roomId, "serverTime", System.currentTimeMillis()));
        messagingTemplate.convertAndSendToUser(username, USER_QUEUE_PREFIX + roomId + "/heartbeat", ack);
    }

    @Override
    public void handleSurrender(String roomId, Long userId) {
        GameMatchState state = getState(roomId);
        GameMatchState.PlayerState playerState = state.findPlayer(userId);
        if (playerState == null) {
            throw new IllegalArgumentException("玩家不在当前牌局中");
        }
        if (playerState.isSurrendered()) {
            return;
        }
        playerState.setSurrendered(true);
        playerState.setAutoPlay(true);
        state.setSurrendering(true);
        persistState(state);
        SurrenderPayload payload = new SurrenderPayload(roomId, state.getMatchId(), userId, 100);
        messagingTemplate.convertAndSend(roomTopic(roomId), GameEventMessage.of(GameEventType.SURRENDER, payload));
    }

    @Override
    public GameMatchState getState(String roomId) {
        String json = stringRedisTemplate.opsForValue().get(RedisConstants.GAME_MATCH_STATE_PREFIX + roomId);
        if (!StringUtils.hasText(json)) {
            throw new IllegalStateException("该房间暂未开始牌局");
        }
        try {
            return objectMapper.readValue(json, GameMatchState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("牌局状态解析失败", e);
        }
    }

    private void persistState(GameMatchState state) {
        state.setUpdatedAt(Instant.now());
        try {
            String json = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.GAME_MATCH_STATE_PREFIX + state.getRoomId(),
                    json,
                    Duration.ofSeconds(RedisConstants.GAME_MATCH_STATE_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("牌局状态序列化失败", e);
        }
    }

    private void broadcastRoomStarted(GameMatchState state) {
        GameEventMessage event = GameEventMessage.of(GameEventType.ROOM_STARTED,
                Map.of("roomId", state.getRoomId(),
                        "matchId", state.getMatchId(),
                        "phase", state.getPhase()));
        messagingTemplate.convertAndSend(roomTopic(state.getRoomId()), event);
    }

    private void pushHands(GameMatchState state) {
        for (GameMatchState.PlayerState playerState : state.getPlayers()) {
            CardsDealtPayload payload = new CardsDealtPayload(
                    state.getRoomId(),
                    state.getMatchId(),
                    playerState.getRole(),
                    playerState.getHandCards(),
                    state.getLandlordCards()
            );
            messagingTemplate.convertAndSendToUser(resolveUsername(playerState.getUserId()),
                    USER_QUEUE_PREFIX + state.getRoomId() + "/cards",
                    GameEventMessage.of(GameEventType.CARDS_DEALT, payload));
        }
    }

    private PlayerSnapshotVO toPlayerSnapshot(GameMatchState.PlayerState playerState, Long requesterId) {
        List<String> handCardsView = playerState.getUserId().equals(requesterId)
                ? playerState.getHandCards()
                : List.of();
        return new PlayerSnapshotVO(
                playerState.getUserId(),
                playerState.getRole(),
                playerState.getHandCards().size(),
                handCardsView,
                playerState.isAutoPlay(),
                playerState.isSurrendered(),
                playerState.getScoreDelta()
        );
    }

    private String roomTopic(String roomId) {
        return "/topic/rooms/" + roomId;
    }

    private String resolveUsername(Long userId) {
        return usernameCache.computeIfAbsent(userId, id -> {
            UserAccount userAccount = userAccountMapper.selectById(id);
            if (userAccount == null) {
                throw new IllegalStateException("未找到用户: " + id);
            }
            return userAccount.getUsername();
        });
    }
}
