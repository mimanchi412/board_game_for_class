package com.xiarui.board_game_backend.game.service.impl;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.common.entity.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiarui.board_game_backend.game.entity.GameMatchEntity;
import com.xiarui.board_game_backend.game.entity.GameMatchPlayerEntity;
import com.xiarui.board_game_backend.game.entity.GameMoveEntity;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryDetailVO;
import com.xiarui.board_game_backend.game.entity.vo.GameHistoryListItemVO;
import com.xiarui.board_game_backend.game.mapper.GameMatchMapper;
import com.xiarui.board_game_backend.game.mapper.GameMatchPlayerMapper;
import com.xiarui.board_game_backend.game.mapper.GameMoveMapper;
import com.xiarui.board_game_backend.game.service.GameHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameHistoryServiceImpl implements GameHistoryService {

    private final GameMatchPlayerMapper gameMatchPlayerMapper;
    private final GameMatchMapper gameMatchMapper;
    private final GameMoveMapper gameMoveMapper;
    private final UserAccountMapper userAccountMapper;

    @Override
    public PageResult<GameHistoryListItemVO> listMyHistory(long page, long size) {
        long current = Math.max(page, 1);
        long pageSize = Math.max(size, 1);
        long offset = (current - 1) * pageSize;
        Long userId = getCurrentUserId();
        List<GameHistoryListItemVO> rows = gameMatchPlayerMapper.selectHistoryByUser(userId, offset, pageSize);
        long total = gameMatchPlayerMapper.countHistoryByUser(userId);
        return PageResult.success(current, pageSize, total, rows);
    }

    @Override
    public GameHistoryDetailVO getMatchDetail(Long matchId) {
        GameMatchEntity match = gameMatchMapper.selectById(matchId);
        if (match == null) {
            throw new IllegalArgumentException("对局不存在");
        }
        List<GameMatchPlayerEntity> players = gameMatchPlayerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GameMatchPlayerEntity>()
                        .eq("match_id", matchId)
                        .orderByAsc("seat"));
        if (CollectionUtils.isEmpty(players)) {
            throw new IllegalArgumentException("对局玩家信息缺失");
        }
        List<GameMoveEntity> moves = gameMoveMapper.selectList(new LambdaQueryWrapper<GameMoveEntity>()
                .eq(GameMoveEntity::getMatchId, matchId)
                .orderByAsc(GameMoveEntity::getStepNo));
        GameHistoryDetailVO vo = new GameHistoryDetailVO();
        vo.setMatchId(matchId);
        vo.setLandlordUserId(match.getLandlordUserId());
        vo.setWinnerSide(match.getWinnerSide());
        vo.setStartTime(match.getStartTime());
        vo.setEndTime(match.getEndTime());
        vo.setRemark(match.getRemark());
        vo.setPlayers(players.stream().map(p -> {
            GameHistoryDetailVO.PlayerResult pr = new GameHistoryDetailVO.PlayerResult();
            pr.setUserId(p.getUserId());
            pr.setSeat(p.getSeat());
            pr.setRole(p.getRole());
            pr.setResult(p.getResult());
            pr.setScoreDelta(p.getScoreDelta());
            pr.setBombs(p.getBombs());
            pr.setLeftCards(p.getLeftCards());
            pr.setDurationSec(p.getDurationSec());
            pr.setEscaped(p.getEscaped());
            return pr;
        }).collect(Collectors.toList()));
        vo.setMoves(moves.stream().map(m -> {
            GameHistoryDetailVO.MoveRecord mr = new GameHistoryDetailVO.MoveRecord();
            mr.setStepNo(m.getStepNo());
            mr.setPlayerId(m.getPlayerId());
            mr.setSeat(m.getSeat());
            mr.setPattern(m.getPattern());
            mr.setCards(m.getCards());
            mr.setBeatsPrev(m.getBeatsPrev());
            mr.setCreatedAt(m.getCreatedAt());
            return mr;
        }).collect(Collectors.toList()));
        return vo;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("未登录");
        }
        UserAccount user = userAccountMapper.findByUsername(authentication.getName());
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        return user.getId();
    }
}
