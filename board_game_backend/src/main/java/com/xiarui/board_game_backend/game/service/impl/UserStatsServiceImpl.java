package com.xiarui.board_game_backend.game.service.impl;

import com.xiarui.board_game_backend.auth.entity.UserAccount;
import com.xiarui.board_game_backend.auth.mapper.UserAccountMapper;
import com.xiarui.board_game_backend.common.entity.PageResult;
import com.xiarui.board_game_backend.game.entity.dto.UserStatsBatchRequest;
import com.xiarui.board_game_backend.game.entity.vo.UserStatsVO;
import com.xiarui.board_game_backend.game.mapper.UserStatsMapper;
import com.xiarui.board_game_backend.game.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户战绩统计服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final UserStatsMapper userStatsMapper;
    private final UserAccountMapper userAccountMapper;

    @Override
    public UserStatsVO getCurrentUserStats() {
        Long userId = getCurrentUserId();
        return ensureStatsExists(userStatsMapper.selectStatsByUserId(userId), userId);
    }

    @Override
    public UserStatsVO getUserStats(Long userId) {
        return ensureStatsExists(userStatsMapper.selectStatsByUserId(userId), userId);
    }

    @Override
    public PageResult<UserStatsVO> getLeaderboard(long page, long size) {
        long current = Math.max(page, 1);
        long pageSize = Math.max(size, 1);
        long offset = (current - 1) * pageSize;
        List<UserStatsVO> rows = userStatsMapper.selectLeaderboard(offset, pageSize);
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(offset + i + 1);
        }
        long total = userStatsMapper.countAll();
        return PageResult.success(current, pageSize, total, rows);
    }

    @Override
    public List<UserStatsVO> getAroundMeLeaderboard(int radius) {
        Long userId = getCurrentUserId();
        Long rank = userStatsMapper.selectRankByUserId(userId);
        if (rank == null) {
            throw new RuntimeException("未找到用户战绩记录");
        }
        int window = Math.max(radius, 1);
        long startRank = Math.max(rank - window, 1);
        long offset = startRank - 1;
        long limit = window * 2L + 1;
        List<UserStatsVO> rows = userStatsMapper.selectLeaderboard(offset, limit);
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(offset + i + 1);
        }
        return rows;
    }

    @Override
    public List<UserStatsVO> getBatchStats(UserStatsBatchRequest request) {
        List<Long> userIds = request.getUserIds();
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserStatsVO> records = userStatsMapper.selectByUserIds(userIds);
        Map<Long, UserStatsVO> recordMap = records.stream()
                .collect(Collectors.toMap(UserStatsVO::getUserId, vo -> vo));

        List<UserStatsVO> result = new ArrayList<>();
        for (Long userId : userIds) {
            UserStatsVO vo = recordMap.get(userId);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("未登录");
        }
        UserAccount userAccount = userAccountMapper.findByUsername(authentication.getName());
        if (userAccount == null) {
            throw new RuntimeException("用户不存在");
        }
        return userAccount.getId();
    }

    private UserStatsVO ensureStatsExists(UserStatsVO vo, Long userId) {
        if (vo == null) {
            throw new RuntimeException("未找到用户战绩记录");
        }
        return vo;
    }
}
