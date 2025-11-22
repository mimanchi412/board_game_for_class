package com.xiarui.board_game_backend.game.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.common.config.GameSettingsProperties;
import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.game.entity.GameMatchEntity;
import com.xiarui.board_game_backend.game.entity.GameMatchPlayerEntity;
import com.xiarui.board_game_backend.game.entity.GameMoveEntity;
import com.xiarui.board_game_backend.game.entity.dto.BidActionRequest;
import com.xiarui.board_game_backend.game.entity.dto.PlayCardRequest;
import com.xiarui.board_game_backend.game.entity.enums.GameEventType;
import com.xiarui.board_game_backend.game.entity.enums.GamePhase;
import com.xiarui.board_game_backend.game.entity.enums.PlayerRole;
import com.xiarui.board_game_backend.game.entity.model.CardCombo;
import com.xiarui.board_game_backend.game.entity.model.GameMatchState;
import com.xiarui.board_game_backend.game.entity.model.GameMatchState.MoveRecord;
import com.xiarui.board_game_backend.game.entity.model.GameRoomInfo;
import com.xiarui.board_game_backend.game.entity.vo.CardsDealtPayload;
import com.xiarui.board_game_backend.game.entity.vo.GameEventMessage;
import com.xiarui.board_game_backend.game.entity.vo.GameSnapshotVO;
import com.xiarui.board_game_backend.game.entity.vo.LastPlaySnapshotVO;
import com.xiarui.board_game_backend.game.entity.vo.PlayerSnapshotVO;
import com.xiarui.board_game_backend.game.entity.vo.SurrenderPayload;
import com.xiarui.board_game_backend.game.mapper.GameMatchMapper;
import com.xiarui.board_game_backend.game.mapper.GameMatchPlayerMapper;
import com.xiarui.board_game_backend.game.mapper.GameMoveMapper;
import com.xiarui.board_game_backend.game.mapper.UserStatsMapper;
import com.xiarui.board_game_backend.game.entity.UserStatsEntity;
import com.xiarui.board_game_backend.game.service.GameMatchService;
import com.xiarui.board_game_backend.game.util.CardValidator;
import com.xiarui.board_game_backend.game.util.DeckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 牌局管理实现，负责发牌、快照、心跳等基础能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameMatchServiceImpl implements GameMatchService {

    private static final String USER_QUEUE_PREFIX = "/queue/room/";
    private static final int BASE_SCORE = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserAccountMapper userAccountMapper;
    private final GameSettingsProperties gameSettings;
    private final GameMatchMapper gameMatchMapper;
    private final GameMatchPlayerMapper gameMatchPlayerMapper;
    private final GameMoveMapper gameMoveMapper;
    private final UserStatsMapper userStatsMapper;

    private final ConcurrentHashMap<Long, String> usernameCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService bidTimeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "bid-timeout");
        t.setDaemon(true);
        return t;
    });
    private final ConcurrentHashMap<String, Future<?>> bidTimeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService playTimeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "play-timeout");
        t.setDaemon(true);
        return t;
    });
    private final ConcurrentHashMap<String, Future<?>> playTimeoutTasks = new ConcurrentHashMap<>();

    @Override
    public GameMatchState initializeMatch(GameRoomInfo roomInfo) {
        GameMatchState state = new GameMatchState();
        state.setRoomId(roomInfo.getRoomId());
        state.setMatchId(UUID.randomUUID().toString());
        state.setPhase(GamePhase.BID);
        state.setSeatOrder(new ArrayList<>(roomInfo.getMemberIds()));
        int startIdx = (int) (Math.random() * roomInfo.getMemberIds().size());
        Long startUser = roomInfo.getMemberIds().get(startIdx);
        state.setCurrentTurnUserId(startUser);
        state.setTurnDeadlineEpochMillis(nextDeadline());
        state.setInitialCallerId(null);
        state.setHighestBidderId(null);
        state.setRemainingBidTurns(roomInfo.getMemberIds().size());
        state.setBidMultiplier(1);
        state.setConsecutiveNoRob(0);
        state.getBidMap().clear();
        state.getRobMap().clear();
        state.setStartTime(Instant.now());
        state.setNextStepNo(1);
        state.getMoves().clear();

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
        broadcastTurnStart(state);
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
                Duration.ofSeconds(gameSettings.getTimeout().getHeartbeatSeconds()));
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
    public void handleOfflineTimeout(String roomId, Long userId) {
        GameMatchState state = getState(roomId);
        GameMatchState.PlayerState ps = state.findPlayer(userId);
        if (ps == null) {
            throw new IllegalArgumentException("玩家不在当前牌局中");
        }
        int count = state.getOfflineTimeoutCount().getOrDefault(userId, 0) + 1;
        state.getOfflineTimeoutCount().put(userId, count);
        if (count >= gameSettings.getOffline().getMaxTimeoutBeforeEscape()) {
            ps.setEscaped(true);
            persistState(state);
            handleSurrender(roomId, userId);
            return;
        }
        ps.setAutoPlay(true);
        persistState(state);
        messagingTemplate.convertAndSend(roomTopic(roomId),
                GameEventMessage.of(GameEventType.AUTO_PLAY,
                        Map.of("roomId", roomId, "userId", userId, "reason", "OFFLINE_TIMEOUT")));
    }

    @Override
    public void handleBid(String roomId, Long userId, BidActionRequest request) {
        if (request == null || request.getCallLandlord() == null) {
            throw new IllegalArgumentException("请求参数缺失");
        }
        GameMatchState state = getState(roomId);
        requirePhase(state, GamePhase.BID, "当前不在叫地主阶段");
        requireTurn(state, userId);
        boolean call = Boolean.TRUE.equals(request.getCallLandlord());
        // 第一阶段：叫地主。有人叫立即进入抢地主阶段；无人叫则流局重发。
        if (state.getInitialCallerId() == null) {
            state.getBidMap().put(userId, call);
            messagingTemplate.convertAndSend(roomTopic(roomId), GameEventMessage.of(
                    GameEventType.BID_RESULT,
                    Map.of("roomId", roomId, "userId", userId, "callLandlord", call)));
            if (call) {
                // 首叫即时进入抢地主阶段
                state.setInitialCallerId(userId);
                state.setHighestBidderId(userId);
                state.setConsecutiveNoRob(0);
                state.getRobMap().clear();
                Long next = nextRobCandidate(state, userId);
                if (next == null) {
                    assignLandlord(state, state.getHighestBidderId());
                    persistState(state);
                    broadcastTurnStart(state);
                    return;
                }
                state.setCurrentTurnUserId(next);
                state.setTurnDeadlineEpochMillis(nextDeadline());
                persistState(state);
                broadcastTurnStart(state);
                return;
            }
            // 一圈都不叫则重新发牌
            if (state.getBidMap().size() >= state.getSeatOrder().size()) {
                redeal(state);
                persistState(state);
                broadcastRoomStarted(state);
                pushHands(state);
                return;
            }
            advanceTurn(state, userId);
            return;
        }

        // 第二阶段：抢地主。每人只能表态一次，出现连续两次“不抢”则确定当前候选为地主。
        if (state.getRobMap().containsKey(userId)) {
            Long next = nextRobCandidate(state, userId);
            if (next == null) {
                Long target = state.getHighestBidderId() != null ? state.getHighestBidderId() : state.getInitialCallerId();
                assignLandlord(state, target);
                persistState(state);
                broadcastTurnStart(state);
            } else {
                state.setCurrentTurnUserId(next);
                state.setTurnDeadlineEpochMillis(nextDeadline());
                persistState(state);
                broadcastTurnStart(state);
            }
            return;
        }
        state.getRobMap().put(userId, call);
        messagingTemplate.convertAndSend(roomTopic(roomId), GameEventMessage.of(
                GameEventType.BID_RESULT,
                Map.of("roomId", roomId, "userId", userId, "callLandlord", call)));

        if (call) {
            state.setHighestBidderId(userId);
            state.setBidMultiplier(state.getBidMultiplier() * 2);
            state.setConsecutiveNoRob(0);
        } else {
            state.setConsecutiveNoRob(state.getConsecutiveNoRob() + 1);
        }

        if (state.getConsecutiveNoRob() >= 2) {
            Long target = state.getHighestBidderId() != null ? state.getHighestBidderId() : state.getInitialCallerId();
            assignLandlord(state, target);
            persistState(state);
            broadcastTurnStart(state);
            return;
        }

        Long next = nextRobCandidate(state, userId);
        if (next == null) {
            Long target = state.getHighestBidderId() != null ? state.getHighestBidderId() : state.getInitialCallerId();
            assignLandlord(state, target);
            persistState(state);
            broadcastTurnStart(state);
            return;
        }
        state.setCurrentTurnUserId(next);
        state.setTurnDeadlineEpochMillis(nextDeadline());
        persistState(state);
        broadcastTurnStart(state);
    }

    @Override
    public void handlePlay(String roomId, Long userId, PlayCardRequest request) {
        if (request == null || request.getCards() == null || request.getCards().isEmpty()) {
            throw new IllegalArgumentException("出牌不能为空");
        }
        GameMatchState state = getState(roomId);
        requirePhase(state, GamePhase.PLAY, "当前不在出牌阶段");
        requireTurn(state, userId);

        GameMatchState.PlayerState playerState = state.findPlayer(userId);
        ensurePlayer(state, playerState);
        CardCombo currentCombo = CardValidator.parse(request.getCards());
        GameMatchState.LastPlay lastPlayRef = state.getLastPlay();
        boolean beatsPrev = true;
        if (lastPlayRef != null && !userId.equals(lastPlayRef.getUserId())) {
            CardCombo lastCombo = CardValidator.parse(lastPlayRef.getCards());
            if (!CardValidator.canBeat(currentCombo, lastCombo)) {
                throw new IllegalArgumentException("出牌未能压上上一手");
            }
            beatsPrev = CardValidator.canBeat(currentCombo, lastCombo);
        }
        removeCards(playerState, request.getCards());
        String pattern = currentCombo.getType().name();
        recordMove(state, userId, pattern, request.getCards(), beatsPrev);

        GameMatchState.LastPlay lastPlay = new GameMatchState.LastPlay();
        lastPlay.setUserId(userId);
        lastPlay.setCards(request.getCards());
        lastPlay.setPattern(pattern);
        lastPlay.setPlayedAtEpochMillis(System.currentTimeMillis());
        state.setLastPlay(lastPlay);

        if (playerState.getHandCards().isEmpty()) {
            persistState(state);
            settle(state, userId);
            broadcastPlay(roomId, request.getCards(), pattern, userId);
            return;
        }

        Long next = nextActiveUser(state, userId);
        state.setCurrentTurnUserId(next);
        state.setTurnDeadlineEpochMillis(nextDeadline());
        persistState(state);
        broadcastPlay(roomId, request.getCards(), pattern, userId);
        broadcastTurnStart(state);
    }

    @Override
    public void handlePass(String roomId, Long userId) {
        GameMatchState state = getState(roomId);
        requirePhase(state, GamePhase.PLAY, "当前不在出牌阶段");
        requireTurn(state, userId);

        recordMove(state, userId, "PASS", List.of(), false);

        Long next = nextActiveUser(state, userId);
        state.setCurrentTurnUserId(next);
        state.setTurnDeadlineEpochMillis(nextDeadline());
        persistState(state);
        messagingTemplate.convertAndSend(roomTopic(roomId),
                GameEventMessage.of(GameEventType.PASS, Map.of("roomId", roomId, "userId", userId)));
        broadcastTurnStart(state);
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
                    Duration.ofSeconds(gameSettings.getMatchState().getActiveTtlSeconds()));
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

    private void broadcastTurnStart(GameMatchState state) {
        GameEventMessage msg = GameEventMessage.of(GameEventType.TURN_START,
                Map.of("roomId", state.getRoomId(),
                        "userId", state.getCurrentTurnUserId(),
                        "turnDeadline", state.getTurnDeadlineEpochMillis()));
        messagingTemplate.convertAndSend(roomTopic(state.getRoomId()), msg);
        if (state.getPhase() == GamePhase.BID) {
            scheduleBidTimeout(state);
        } else {
            cancelBidTimeout(state.getRoomId());
            schedulePlayTimeout(state);
        }
    }

    private void broadcastPlay(String roomId, List<String> cards, String pattern, Long userId) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("roomId", roomId);
        payload.put("userId", userId);
        payload.put("cards", cards);
        if (pattern != null) {
            payload.put("pattern", pattern);
        }
        messagingTemplate.convertAndSend(roomTopic(roomId), GameEventMessage.of(GameEventType.PLAY_CARD, payload));
    }

    private void assignLandlord(GameMatchState state, Long landlordId) {
        cancelBidTimeout(state.getRoomId());
        state.setLandlordId(landlordId);
        state.setPhase(GamePhase.PLAY);
        state.setInitialCallerId(null);
        state.setHighestBidderId(null);
        state.setRemainingBidTurns(0);
        state.setBidMultiplier(state.getBidMultiplier());
        for (GameMatchState.PlayerState ps : state.getPlayers()) {
            if (ps.getUserId().equals(landlordId)) {
                ps.setRole(PlayerRole.LANDLORD);
                ps.getHandCards().addAll(state.getLandlordCards());
                DeckUtil.sortCards(ps.getHandCards());
            } else {
                ps.setRole(PlayerRole.FARMER);
            }
        }
        state.setCurrentTurnUserId(landlordId);
        state.setTurnDeadlineEpochMillis(nextDeadline());
    }

    private void redeal(GameMatchState state) {
        List<String> deck = DeckUtil.shuffledDeck();
        state.setPhase(GamePhase.BID);
        state.setLandlordId(null);
        state.setInitialCallerId(null);
        state.setHighestBidderId(null);
        state.setRemainingBidTurns(state.getSeatOrder().size());
        state.setBidMultiplier(1);
        state.setLastPlay(null);
        state.setTurnDeadlineEpochMillis(nextDeadline());
        state.setMatchId(UUID.randomUUID().toString());
        state.setCurrentTurnUserId(state.getSeatOrder().get(0));
        state.getBidMap().clear();
        state.getRobMap().clear();
        state.setConsecutiveNoRob(0);
        state.getLandlordCards().clear();
        state.getOfflineTimeoutCount().clear();
        state.getPlayers().forEach(ps -> {
            ps.getHandCards().clear();
            ps.setRole(PlayerRole.UNKNOWN);
            ps.setAutoPlay(false);
            ps.setSurrendered(false);
            ps.setEscaped(false);
        });
        for (int i = 0; i < state.getSeatOrder().size(); i++) {
            Long uid = state.getSeatOrder().get(i);
            GameMatchState.PlayerState ps = state.findPlayer(uid);
            if (ps == null) {
                ps = new GameMatchState.PlayerState();
                ps.setUserId(uid);
                state.getPlayers().add(ps);
            }
            int from = i * 17;
            int to = Math.min(from + 17, deck.size());
            ps.getHandCards().addAll(deck.subList(from, to));
            DeckUtil.sortCards(ps.getHandCards());
        }
        state.getLandlordCards().addAll(deck.subList(51, 54));
    }

    private void settle(GameMatchState state, Long winnerId) {
        cancelBidTimeout(state.getRoomId());
        cancelPlayTimeout(state.getRoomId());
        state.setPhase(GamePhase.SETTLEMENT);
        state.setCurrentTurnUserId(winnerId);
        Instant endTime = Instant.now();
        state.setEndTime(endTime);

        Long landlordId = state.getLandlordId();
        String winnerSide = winnerId.equals(landlordId) ? "LANDLORD" : "FARMER";
        Map<Long, Integer> bombsByUser = new java.util.HashMap<>();
        int multiplier = Math.max(1, state.getBidMultiplier());
        for (MoveRecord record : state.getMoves()) {
            if (record == null || record.getPattern() == null) {
                continue;
            }
            switch (record.getPattern()) {
                case "BOMB" -> {
                    multiplier *= 2;
                    bombsByUser.merge(record.getUserId(), 1, Integer::sum);
                }
                case "JOKER_BOMB" -> {
                    multiplier *= 4;
                    bombsByUser.merge(record.getUserId(), 1, Integer::sum);
                }
                default -> {
                }
            }
        }
        boolean spring = detectSpring(state, winnerSide);
        if (spring) {
            multiplier *= 2;
        }

        int scoreUnit = BASE_SCORE * multiplier;
        Map<Long, Integer> scoreDelta = new java.util.HashMap<>();
        for (GameMatchState.PlayerState ps : state.getPlayers()) {
            boolean isLandlord = ps.getUserId().equals(landlordId);
            boolean isWinner = "LANDLORD".equals(winnerSide) ? isLandlord : !isLandlord;
            int delta = isWinner
                    ? ("LANDLORD".equals(winnerSide) ? scoreUnit * 2 : scoreUnit)
                    : ("LANDLORD".equals(winnerSide) ? -scoreUnit : -scoreUnit * 2);
            if (ps.isSurrendered()) {
                delta -= gameSettings.getPenalty().getSurrender();
            }
            if (ps.isEscaped()) {
                delta -= gameSettings.getPenalty().getEscape();
            }
            scoreDelta.put(ps.getUserId(), delta);
            ps.setScoreDelta(delta);
        }

        persistMatchResult(state, winnerSide, scoreDelta, bombsByUser, scoreUnit, spring, endTime);
        persistState(state);
        // 结算后缩短状态保留时间，避免 Redis 长时间占用。
        long ttlMinutes = gameSettings.getMatchState().getSettledTtlMinutes();
        if (ttlMinutes > 0) {
            stringRedisTemplate.expire(RedisConstants.GAME_MATCH_STATE_PREFIX + state.getRoomId(),
                    Duration.ofMinutes(ttlMinutes));
        } else {
            stringRedisTemplate.delete(RedisConstants.GAME_MATCH_STATE_PREFIX + state.getRoomId());
        }
        messagingTemplate.convertAndSend(roomTopic(state.getRoomId()),
                GameEventMessage.of(GameEventType.GAME_RESULT,
                        Map.of("roomId", state.getRoomId(),
                                "matchId", state.getMatchId(),
                                "winnerId", winnerId,
                                "landlordId", state.getLandlordId(),
                                "multiplier", multiplier,
                                "spring", spring,
                                "scoreUnit", scoreUnit,
                                "scoreDelta", scoreDelta)));
    }

    private void requirePhase(GameMatchState state, GamePhase expected, String message) {
        if (state.getPhase() != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void requireTurn(GameMatchState state, Long userId) {
        if (!userId.equals(state.getCurrentTurnUserId())) {
            throw new IllegalStateException("当前不是你的回合");
        }
    }

    private void ensurePlayer(GameMatchState state, GameMatchState.PlayerState playerState) {
        if (playerState == null) {
            throw new IllegalArgumentException("玩家不在牌局中");
        }
        if (playerState.isSurrendered()) {
            throw new IllegalStateException("该玩家已托管/投降");
        }
    }

    private Long nextActiveUser(GameMatchState state, Long currentUserId) {
        List<Long> seats = state.getSeatOrder();
        int idx = seats.indexOf(currentUserId);
        for (int i = 1; i <= seats.size(); i++) {
            Long candidate = seats.get((idx + i) % seats.size());
            GameMatchState.PlayerState ps = state.findPlayer(candidate);
            if (ps != null && !ps.isSurrendered()) {
                return candidate;
            }
        }
        return currentUserId;
    }

    private void removeCards(GameMatchState.PlayerState playerState, List<String> cards) {
        for (String card : cards) {
            boolean removed = playerState.getHandCards().remove(card);
            if (!removed) {
                throw new IllegalArgumentException("手牌不足，无法出牌");
            }
        }
    }

    private long nextDeadline() {
        return Instant.now().plusSeconds(gameSettings.getTimeout().getTurnSeconds()).toEpochMilli();
    }

    private void recordMove(GameMatchState state, Long userId, String pattern, List<String> cards, boolean beatsPrev) {
        MoveRecord record = new MoveRecord();
        record.setStepNo(state.getNextStepNo());
        record.setUserId(userId);
        record.setPattern(pattern);
        record.setCards(new ArrayList<>(cards));
        record.setBeatsPrev(beatsPrev);
        record.setCreatedAt(Instant.now());
        state.getMoves().add(record);
        state.setNextStepNo(state.getNextStepNo() + 1);
    }

    private boolean detectSpring(GameMatchState state, String winnerSide) {
        Long landlordId = state.getLandlordId();
        long landlordPlays = state.getMoves().stream()
                .filter(r -> r.getUserId() != null && r.getUserId().equals(landlordId) && !r.isPass())
                .count();
        long farmerPlays = state.getMoves().stream()
                .filter(r -> r.getUserId() != null && !r.getUserId().equals(landlordId) && !r.isPass())
                .count();
        if ("LANDLORD".equals(winnerSide)) {
            return farmerPlays == 0;
        }
        return landlordPlays <= 1;
    }

    private void persistMatchResult(GameMatchState state,
                                    String winnerSide,
                                    Map<Long, Integer> scoreDelta,
                                    Map<Long, Integer> bombsByUser,
                                    int scoreUnit,
                                    boolean spring,
                                    Instant endTime) {
        GameMatchEntity match = new GameMatchEntity();
        match.setRoomId(state.getRoomId());
        match.setLandlordUserId(state.getLandlordId());
        match.setWinnerSide(winnerSide);
        match.setStartTime(toOffset(state.getStartTime()));
        match.setEndTime(toOffset(endTime));
        try {
            Map<String, Object> remark = new java.util.HashMap<>();
            remark.put("bidMultiplier", state.getBidMultiplier());
            remark.put("scoreUnit", scoreUnit);
            remark.put("spring", spring);
            match.setRemark(objectMapper.writeValueAsString(remark));
        } catch (JsonProcessingException e) {
            match.setRemark("{\"scoreUnit\":" + scoreUnit + "}");
        }
        gameMatchMapper.insert(match);
        Long matchId = match.getId();
        persistMoves(state, matchId);
        persistPlayers(state, winnerSide, scoreDelta, bombsByUser, matchId, endTime);
        updateUserStats(state, winnerSide, scoreDelta);
    }

    private void persistMoves(GameMatchState state, Long matchId) {
        for (MoveRecord record : state.getMoves()) {
            if (record == null) {
                continue;
            }
            GameMoveEntity entity = new GameMoveEntity();
            entity.setMatchId(matchId);
            entity.setStepNo(record.getStepNo());
            entity.setPlayerId(record.getUserId());
            entity.setSeat(getSeat(state, record.getUserId()));
            entity.setPattern(record.getPattern());
            entity.setCards(record.getCards());
            entity.setBeatsPrev(record.isBeatsPrev());
            entity.setCreatedAt(toOffset(record.getCreatedAt()));
            gameMoveMapper.insert(entity);
        }
    }

    private void persistPlayers(GameMatchState state,
                                String winnerSide,
                                Map<Long, Integer> scoreDelta,
                                Map<Long, Integer> bombsByUser,
                                Long matchId,
                                Instant endTime) {
        int durationSec = 0;
        if (state.getStartTime() != null) {
            durationSec = (int) Duration.between(state.getStartTime(), endTime).getSeconds();
        }
        Long landlordId = state.getLandlordId();
        for (GameMatchState.PlayerState ps : state.getPlayers()) {
            GameMatchPlayerEntity entity = new GameMatchPlayerEntity();
            entity.setMatchId(matchId);
            entity.setUserId(ps.getUserId());
            entity.setSeat(getSeat(state, ps.getUserId()));
            entity.setRole(ps.getRole().name());
            boolean isLandlord = ps.getUserId().equals(landlordId);
            boolean isWinner = "LANDLORD".equals(winnerSide) ? isLandlord : !isLandlord;
            entity.setResult(isWinner ? "WIN" : "LOSE");
            entity.setScoreDelta(scoreDelta.getOrDefault(ps.getUserId(), 0));
            entity.setBombs(bombsByUser.getOrDefault(ps.getUserId(), 0));
            entity.setLeftCards(ps.getHandCards().size());
            entity.setDurationSec(durationSec);
            entity.setEscaped(ps.isEscaped());
            gameMatchPlayerMapper.insert(entity);
        }
    }

    private void updateUserStats(GameMatchState state, String winnerSide, Map<Long, Integer> scoreDelta) {
        Long landlordId = state.getLandlordId();
        for (GameMatchState.PlayerState ps : state.getPlayers()) {
            boolean isLandlord = ps.getUserId().equals(landlordId);
            boolean win = "LANDLORD".equals(winnerSide) ? isLandlord : !isLandlord;
            int delta = scoreDelta.getOrDefault(ps.getUserId(), 0);
            UserStatsEntity stats = userStatsMapper.selectById(ps.getUserId());
            if (stats == null) {
                stats = new UserStatsEntity();
                stats.setUserId(ps.getUserId());
                stats.setTotalGames(0);
                stats.setWinCount(0);
                stats.setLoseCount(0);
                stats.setScore(1000);
                stats.setLevel(1);
                stats.setMaxStreak(0);
                stats.setCurrentStreak(0);
            }
            stats.setTotalGames((stats.getTotalGames() == null ? 0 : stats.getTotalGames()) + 1);
            if (win) {
                stats.setWinCount((stats.getWinCount() == null ? 0 : stats.getWinCount()) + 1);
                int streak = (stats.getCurrentStreak() == null ? 0 : stats.getCurrentStreak()) + 1;
                stats.setCurrentStreak(streak);
                stats.setMaxStreak(Math.max(stats.getMaxStreak() == null ? 0 : stats.getMaxStreak(), streak));
            } else {
                stats.setLoseCount((stats.getLoseCount() == null ? 0 : stats.getLoseCount()) + 1);
                stats.setCurrentStreak(0);
            }
            int newScore = Math.max(0, (stats.getScore() == null ? 1000 : stats.getScore()) + delta);
            stats.setScore(newScore);
            stats.setLevel(calculateLevel(newScore));
            stats.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
            if (userStatsMapper.selectById(ps.getUserId()) == null) {
                userStatsMapper.insert(stats);
            } else {
                userStatsMapper.updateById(stats);
            }
        }
    }

    private int getSeat(GameMatchState state, Long userId) {
        int idx = state.getSeatOrder().indexOf(userId);
        if (idx < 0) {
            throw new IllegalStateException("未找到玩家座位：" + userId);
        }
        return idx;
    }

    private OffsetDateTime toOffset(Instant instant) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private int calculateLevel(int score) {
        int level = 1;
        int threshold = 1000;
        while (score >= threshold * 2) {
            threshold *= 2;
            level++;
        }
        return level;
    }

    /**
     * 切换到下一位玩家并广播回合开始。
     */
    private void advanceTurn(GameMatchState state, Long currentUserId) {
        Long next = nextActiveUser(state, currentUserId);
        state.setCurrentTurnUserId(next);
        state.setTurnDeadlineEpochMillis(nextDeadline());
        persistState(state);
        broadcastTurnStart(state);
    }

    /**
     * 抢地主阶段寻找下一个还未表态的玩家。
     */
    private Long nextRobCandidate(GameMatchState state, Long currentUserId) {
        List<Long> seats = state.getSeatOrder();
        int idx = seats.indexOf(currentUserId);
        for (int i = 1; i <= seats.size(); i++) {
            Long candidate = seats.get((idx + i) % seats.size());
            GameMatchState.PlayerState ps = state.findPlayer(candidate);
            if (ps != null && !ps.isSurrendered() && !state.getRobMap().containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
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

    private void scheduleBidTimeout(GameMatchState state) {
        cancelBidTimeout(state.getRoomId());
        long delayMs = Math.max(0, state.getTurnDeadlineEpochMillis() - System.currentTimeMillis());
        Long userId = state.getCurrentTurnUserId();
        String roomId = state.getRoomId();
        Future<?> future = bidTimeoutExecutor.schedule(() -> {
            try {
                GameMatchState latest = getState(roomId);
                if (latest.getPhase() != GamePhase.BID) {
                    return;
                }
                if (!userId.equals(latest.getCurrentTurnUserId())) {
                    return;
                }
                boolean alreadyCalled = latest.getInitialCallerId() == null
                        ? latest.getBidMap().containsKey(userId)
                        : latest.getRobMap().containsKey(userId);
                if (alreadyCalled) {
                    return;
                }
                BidActionRequest req = new BidActionRequest();
                req.setCallLandlord(false);
                handleBid(roomId, userId, req);
                log.info("Bid timeout auto-pass, room={}, user={}", roomId, userId);
            } catch (Exception e) {
                log.warn("Bid timeout auto-pass failed, room={}, user={}", roomId, userId, e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        bidTimeoutTasks.put(roomId, future);
    }

    private void cancelBidTimeout(String roomId) {
        Future<?> future = bidTimeoutTasks.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void autoPlayTimeout(GameMatchState state, Long userId) {
        GameMatchState.LastPlay last = state.getLastPlay();
        boolean canPass = last != null && !userId.equals(last.getUserId());
        if (canPass) {
            handlePass(state.getRoomId(), userId);
            return;
        }
        GameMatchState.PlayerState ps = state.findPlayer(userId);
        if (ps == null || ps.getHandCards().isEmpty()) {
            handlePass(state.getRoomId(), userId);
            return;
        }
        List<String> cards = List.of(ps.getHandCards().get(0));
        PlayCardRequest req = new PlayCardRequest();
        req.setCards(cards);
        handlePlay(state.getRoomId(), userId, req);
    }

    private void schedulePlayTimeout(GameMatchState state) {
        cancelPlayTimeout(state.getRoomId());
        long delayMs = Math.max(0, state.getTurnDeadlineEpochMillis() - System.currentTimeMillis());
        Long userId = state.getCurrentTurnUserId();
        String roomId = state.getRoomId();
        Future<?> future = playTimeoutExecutor.schedule(() -> {
            try {
                GameMatchState latest = getState(roomId);
                if (latest.getPhase() != GamePhase.PLAY) {
                    return;
                }
                if (!userId.equals(latest.getCurrentTurnUserId())) {
                    return;
                }
                autoPlayTimeout(latest, userId);
                log.info("Play timeout auto action, room={}, user={}", roomId, userId);
            } catch (Exception e) {
                log.warn("Play timeout auto action failed, room={}, user={}", roomId, userId, e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        playTimeoutTasks.put(roomId, future);
    }

    private void cancelPlayTimeout(String roomId) {
        Future<?> f = playTimeoutTasks.remove(roomId);
        if (f != null) {
            f.cancel(false);
        }
    }
}
