package com.xiarui.board_game_backend.game.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.common.constants.RedisConstants;
import com.xiarui.board_game_backend.game.entity.dto.CreateCustomRoomRequest;
import com.xiarui.board_game_backend.game.entity.dto.JoinRoomByCodeRequest;
import com.xiarui.board_game_backend.game.entity.dto.ReadyRequest;
import com.xiarui.board_game_backend.game.entity.enums.GameRoomMode;
import com.xiarui.board_game_backend.game.entity.enums.GameRoomStatus;
import com.xiarui.board_game_backend.game.entity.model.GameRoomInfo;
import com.xiarui.board_game_backend.game.entity.vo.GameRoomVO;
import com.xiarui.board_game_backend.game.entity.vo.MatchJoinResponse;
import com.xiarui.board_game_backend.game.service.GameMatchService;
import com.xiarui.board_game_backend.game.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 房间/匹配服务实现，主要依赖 Redis 管理房间状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final UserAccountMapper userAccountMapper;
    private final GameMatchService gameMatchService;

    private static final int ROOM_CAPACITY = 3;

    @Override
    public MatchJoinResponse joinRandomMatch() {
        Long userId = getCurrentUserId();
        ensureNotInRoom(userId);
        stringRedisTemplate.opsForSet().add(RedisConstants.GAME_MATCH_POOL_KEY, String.valueOf(userId));
        stringRedisTemplate.expire(RedisConstants.GAME_MATCH_POOL_KEY, RedisConstants.GAME_MATCH_WAIT_SECONDS, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(RedisConstants.GAME_MATCH_TICKET_PREFIX + userId, "1",
                Duration.ofSeconds(RedisConstants.GAME_MATCH_WAIT_SECONDS));

        MatchJoinResponse response = new MatchJoinResponse();
        if (tryFormMatch()) {
            GameRoomInfo room = getRoomByMember(userId);
            response.setMatched(true);
            response.setRoom(toVO(room));
            response.setMessage("匹配成功");
        } else {
            response.setMatched(false);
            response.setMessage("匹配中，等待其他玩家...");
        }
        return response;
    }

    @Override
    public GameRoomVO createCustomRoom(CreateCustomRoomRequest request) {
        Long userId = getCurrentUserId();
        rateLimitRoomCreate(userId);
        ensureNotInRoom(userId);
        String roomId = UUID.randomUUID().toString();
        String roomCode = generateRoomCode();
        GameRoomInfo roomInfo = new GameRoomInfo();
        roomInfo.setRoomId(roomId);
        roomInfo.setRoomCode(roomCode);
        roomInfo.setMode(GameRoomMode.CUSTOM);
        roomInfo.setStatus(GameRoomStatus.WAITING);
        roomInfo.setOwnerId(userId);
        roomInfo.getMemberIds().add(userId);
        roomInfo.getReadyMap().put(userId, false);
        saveRoom(roomInfo);
        mapRoomCode(roomCode, roomId);
        return toVO(roomInfo);
    }

    @Override
    public GameRoomVO joinByCode(JoinRoomByCodeRequest request) {
        Long userId = getCurrentUserId();
        ensureNotInRoom(userId);
        String roomId = stringRedisTemplate.opsForValue().get(RedisConstants.GAME_ROOM_CODE_PREFIX + request.getRoomCode());
        if (!StringUtils.hasText(roomId)) {
            throw new RuntimeException("房间不存在或已过期");
        }
        GameRoomInfo room = loadRoom(roomId);
        if (room.getMemberIds().size() >= ROOM_CAPACITY) {
            throw new RuntimeException("房间已满");
        }
        room.getMemberIds().add(userId);
        room.getReadyMap().put(userId, false);
        updateRoomStatus(room);
        saveRoom(room);
        return toVO(room);
    }

    @Override
    public GameRoomVO toggleReady(String roomId, ReadyRequest request) {
        Long userId = getCurrentUserId();
        GameRoomInfo room = loadRoom(roomId);
        if (!room.getMemberIds().contains(userId)) {
            throw new RuntimeException("不在该房间中");
        }
        room.getReadyMap().put(userId, request.getReady());
        updateRoomStatus(room);
        saveRoom(room);
        return toVO(room);
    }

    @Override
    public GameRoomVO startGame(String roomId) {
        GameRoomInfo room = loadRoom(roomId);
        validateStart(room);
        if (!acquireStartLock(roomId)) {
            throw new RuntimeException("有人正在发起开始，请稍后");
        }
        room.setStatus(GameRoomStatus.PLAYING);
        saveRoom(room);
        gameMatchService.initializeMatch(room);
        log.info("Room {} start game with members {}", roomId, room.getMemberIds());
        return toVO(room);
    }

    @Override
    public GameRoomVO getRoom(String roomId) {
        return toVO(loadRoom(roomId));
    }

    @Override
    public GameRoomVO getMyRoom() {
        Long userId = getCurrentUserId();
        String roomId = stringRedisTemplate.opsForValue().get(RedisConstants.GAME_ROOM_USER_PREFIX + userId);
        if (!StringUtils.hasText(roomId)) {
            throw new RuntimeException("当前没有所在的房间");
        }
        return toVO(loadRoom(roomId));
    }

    private void saveRoom(GameRoomInfo roomInfo) {
        try {
            String key = RedisConstants.GAME_ROOM_INFO_PREFIX + roomInfo.getRoomId();
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(roomInfo),
                    Duration.ofSeconds(RedisConstants.GAME_ROOM_TTL_SECONDS));
            if (StringUtils.hasText(roomInfo.getRoomCode())) {
                mapRoomCode(roomInfo.getRoomCode(), roomInfo.getRoomId());
            }
            bindMembers(roomInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化房间失败", e);
        }
    }

    private void mapRoomCode(String roomCode, String roomId) {
        stringRedisTemplate.opsForValue().set(RedisConstants.GAME_ROOM_CODE_PREFIX + roomCode, roomId,
                Duration.ofSeconds(RedisConstants.GAME_ROOM_TTL_SECONDS));
    }

    private GameRoomInfo loadRoom(String roomId) {
        String json = stringRedisTemplate.opsForValue().get(RedisConstants.GAME_ROOM_INFO_PREFIX + roomId);
        if (!StringUtils.hasText(json)) {
            throw new RuntimeException("房间不存在或已过期");
        }
        try {
            return objectMapper.readValue(json, GameRoomInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("房间数据异常", e);
        }
    }

    private void ensureNotInRoom(Long userId) {
        String roomId = stringRedisTemplate.opsForValue().get(RedisConstants.GAME_ROOM_USER_PREFIX + userId);
        if (StringUtils.hasText(roomId)) {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstants.GAME_ROOM_INFO_PREFIX + roomId))) {
                stringRedisTemplate.delete(RedisConstants.GAME_ROOM_USER_PREFIX + userId);
            } else {
                throw new RuntimeException("已在房间中，不能重复加入");
            }
        }
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstants.GAME_MATCH_TICKET_PREFIX + userId))) {
            throw new RuntimeException("当前正在匹配中");
        }
    }

    private boolean tryFormMatch() {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.GAME_MATCH_LOCK_KEY, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            return false;
        }
        try {
            Long size = stringRedisTemplate.opsForSet().size(RedisConstants.GAME_MATCH_POOL_KEY);
            if (size == null || size < ROOM_CAPACITY) {
                return false;
            }
            List<Long> selected = new ArrayList<>();
            for (int i = 0; i < ROOM_CAPACITY; i++) {
                String member = stringRedisTemplate.opsForSet().pop(RedisConstants.GAME_MATCH_POOL_KEY);
                if (!StringUtils.hasText(member)) {
                    break;
                }
                selected.add(Long.valueOf(member));
            }
            if (selected.size() < ROOM_CAPACITY) {
                stringRedisTemplate.opsForSet().add(RedisConstants.GAME_MATCH_POOL_KEY,
                        selected.stream().map(String::valueOf).toArray(String[]::new));
                return false;
            }
            for (Long uid : selected) {
                stringRedisTemplate.delete(RedisConstants.GAME_MATCH_TICKET_PREFIX + uid);
            }
            createRoomFromMatch(selected);
            return true;
        } finally {
            stringRedisTemplate.delete(RedisConstants.GAME_MATCH_LOCK_KEY);
        }
    }

    private void createRoomFromMatch(List<Long> members) {
        String roomId = UUID.randomUUID().toString();
        GameRoomInfo room = new GameRoomInfo();
        room.setRoomId(roomId);
        room.setMode(GameRoomMode.RANDOM);
        room.setStatus(GameRoomStatus.WAITING);
        room.setOwnerId(members.get(0));
        room.setMemberIds(new ArrayList<>(members));
        for (Long uid : members) {
            room.getReadyMap().put(uid, false);
        }
        saveRoom(room);
    }

    private GameRoomInfo getRoomByMember(Long userId) {
        String roomId = stringRedisTemplate.opsForValue().get(RedisConstants.GAME_ROOM_USER_PREFIX + userId);
        if (!StringUtils.hasText(roomId)) {
            throw new RuntimeException("未找到所在房间");
        }
        return loadRoom(roomId);
    }

    private void updateRoomStatus(GameRoomInfo room) {
        if (room.getMemberIds().size() < ROOM_CAPACITY) {
            room.setStatus(GameRoomStatus.WAITING);
            return;
        }
        boolean allReady = room.getMemberIds().stream().allMatch(id -> Boolean.TRUE.equals(room.getReadyMap().get(id)));
        room.setStatus(allReady ? GameRoomStatus.READY : GameRoomStatus.WAITING);
    }

    private void validateStart(GameRoomInfo room) {
        if (room.getMemberIds().size() < ROOM_CAPACITY) {
            throw new RuntimeException("房间人数不足");
        }
        boolean allReady = room.getMemberIds().stream().allMatch(id -> Boolean.TRUE.equals(room.getReadyMap().get(id)));
        if (!allReady) {
            throw new RuntimeException("仍有玩家未准备");
        }
    }

    private boolean acquireStartLock(String roomId) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisConstants.GAME_ROOM_START_LOCK_PREFIX + roomId, "1", Duration.ofSeconds(10)));
    }

    private void rateLimitRoomCreate(Long userId) {
        String key = RedisConstants.GAME_ROOM_CREATE_LIMIT_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, RedisConstants.GAME_ROOM_CREATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count != null && count > RedisConstants.GAME_ROOM_CREATE_LIMIT_MAX) {
            throw new RuntimeException("创建房间过于频繁，请稍后重试");
        }
    }

    private String generateRoomCode() {
        String code;
        do {
            code = String.format("%06d", (int) (Math.random() * 1_000_000));
        } while (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstants.GAME_ROOM_CODE_PREFIX + code)));
        return code;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new RuntimeException("未登录");
        }
        UserAccount user = userAccountMapper.findByUsername(authentication.getName());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getId();
    }

    private GameRoomVO toVO(GameRoomInfo room) {
        GameRoomVO vo = new GameRoomVO();
        vo.setRoomId(room.getRoomId());
        vo.setRoomCode(room.getRoomCode());
        vo.setMode(room.getMode());
        vo.setStatus(room.getStatus());
        vo.setOwnerId(room.getOwnerId());
        vo.setMemberIds(room.getMemberIds());
        vo.setReadyMap(room.getReadyMap());
        vo.setCreatedAt(room.getCreatedAt());
        return vo;
    }

    private void bindMembers(GameRoomInfo room) {
        Duration ttl = Duration.ofSeconds(RedisConstants.GAME_ROOM_TTL_SECONDS);
        for (Long uid : room.getMemberIds()) {
            stringRedisTemplate.opsForValue().set(RedisConstants.GAME_ROOM_USER_PREFIX + uid, room.getRoomId(), ttl);
        }
    }
    
    @Override
    public void leaveRoom(String roomId) {
        Long userId = getCurrentUserId();
        try {
            GameRoomInfo room = loadRoom(roomId);
            
            // 检查用户是否在房间中
            if (!room.getMemberIds().contains(userId)) {
                throw new RuntimeException("不在该房间中");
            }
            
            // 从房间成员列表中移除用户
            room.getMemberIds().remove(userId);
            // 从准备状态映射中移除用户
            room.getReadyMap().remove(userId);
            
            // 如果房间中没有成员了，删除房间
            if (room.getMemberIds().isEmpty()) {
                deleteRoom(room);
            } else {
                // 如果离开的是房主，重新选举房主
                if (userId.equals(room.getOwnerId())) {
                    room.setOwnerId(room.getMemberIds().get(0));
                }
                
                // 更新房间状态
                updateRoomStatus(room);
                // 保存更新后的房间信息
                saveRoom(room);
            }
        } finally {
            // 无论是否发生异常，都移除用户与房间的绑定关系
            stringRedisTemplate.delete(RedisConstants.GAME_ROOM_USER_PREFIX + userId);
        }
    }
    
    private void deleteRoom(GameRoomInfo room) {
        // 删除房间信息
        stringRedisTemplate.delete(RedisConstants.GAME_ROOM_INFO_PREFIX + room.getRoomId());
        // 如果是自定义房间，删除房间码映射
        if (StringUtils.hasText(room.getRoomCode())) {
            stringRedisTemplate.delete(RedisConstants.GAME_ROOM_CODE_PREFIX + room.getRoomCode());
        }
    }
}
