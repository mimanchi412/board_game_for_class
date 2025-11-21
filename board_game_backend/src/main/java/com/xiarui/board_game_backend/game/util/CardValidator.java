package com.xiarui.board_game_backend.game.util;

import com.xiarui.board_game_backend.game.entity.enums.CardComboType;
import com.xiarui.board_game_backend.game.entity.model.CardCombo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 牌型校验与比较。
 */
public final class CardValidator {

    private static final List<String> RANK_ORDER = List.of(
            "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2", "BJ", "RJ");

    private CardValidator() {
    }

    public static CardCombo parse(List<String> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("出牌不能为空");
        }
        Map<String, Integer> rankCount = countRanks(cards);
        int total = cards.size();

        // Joker bomb
        if (total == 2 && rankCount.size() == 2 && rankCount.containsKey("BJ") && rankCount.containsKey("RJ")) {
            return new CardCombo(CardComboType.JOKER_BOMB, weight("RJ"), total, 1);
        }

        if (rankCount.size() == 1) {
            int count = rankCount.values().iterator().next();
            String rank = rankCount.keySet().iterator().next();
            if (count == 4) {
                return new CardCombo(CardComboType.BOMB, weight(rank), total, 1);
            }
            if (count == 3) {
                return new CardCombo(CardComboType.TRIPS, weight(rank), total, 1);
            }
            if (count == 2) {
                return new CardCombo(CardComboType.PAIR, weight(rank), total, 1);
            }
            return new CardCombo(CardComboType.SINGLE, weight(rank), total, 1);
        }

        // 三带一/对
        if (total == 4 || total == 5) {
            List<String> trips = ranksWithCount(rankCount, 3);
            if (trips.size() == 1) {
                String main = trips.get(0);
                if (total == 4) {
                    return new CardCombo(CardComboType.TRIPS_WITH_SINGLE, weight(main), total, 1);
                }
                if (total == 5 && rankCount.containsValue(2)) {
                    return new CardCombo(CardComboType.TRIPS_WITH_PAIR, weight(main), total, 1);
                }
            }
        }

        // 四带二（两单）
        if (total == 6) {
            List<String> fours = ranksWithCount(rankCount, 4);
            if (fours.size() == 1) {
                return new CardCombo(CardComboType.FOUR_WITH_TWO, weight(fours.get(0)), total, 1);
            }
        }

        // 顺子
        if (total >= 5 && isStraight(rankCount, 1)) {
            int max = maxRank(rankCount.keySet());
            return new CardCombo(CardComboType.STRAIGHT, max, total, total);
        }

        // 连对
        if (total >= 6 && total % 2 == 0 && isStraight(rankCount, 2)) {
            int pairs = total / 2;
            int max = maxRank(rankCount.keySet());
            return new CardCombo(CardComboType.STRAIGHT_PAIRS, max, total, pairs);
        }

        // 飞机、不带或带单/带对
        List<String> trips = ranksWithCount(rankCount, 3);
        if (trips.size() >= 2 && isConsecutive(trips)) {
            int planeLen = trips.size();
            // 不带
            if (total == planeLen * 3) {
                int max = maxRank(trips);
                return new CardCombo(CardComboType.AIRPLANE, max, total, planeLen);
            }
            // 带单
            if (total == planeLen * 4) {
                int others = total - planeLen * 3;
                if (others == planeLen) {
                    int max = maxRank(trips);
                    return new CardCombo(CardComboType.AIRPLANE_WITH_SINGLE, max, total, planeLen);
                }
            }
            // 带对
            if (total == planeLen * 5) {
                if (countPairs(rankCount) == planeLen) {
                    int max = maxRank(trips);
                    return new CardCombo(CardComboType.AIRPLANE_WITH_PAIRS, max, total, planeLen);
                }
            }
        }

        throw new IllegalArgumentException("不支持的牌型");
    }

    public static boolean canBeat(CardCombo current, CardCombo last) {
        if (last == null) {
            return true;
        }
        // 同一玩家重新出牌也允许（在服务层处理），此处只比大小
        if (current.getType() == CardComboType.JOKER_BOMB) {
            return true;
        }
        if (current.getType() == CardComboType.BOMB) {
            if (last.getType() != CardComboType.BOMB && last.getType() != CardComboType.JOKER_BOMB) {
                return true;
            }
        }
        if (current.getType() != last.getType()) {
            return false;
        }
        if (current.getType() == CardComboType.BOMB) {
            return current.getMainRankWeight() > last.getMainRankWeight();
        }
        // 长度、组数需匹配
        if (current.getSize() != last.getSize() || current.getGroupCount() != last.getGroupCount()) {
            return false;
        }
        return current.getMainRankWeight() > last.getMainRankWeight();
    }

    private static Map<String, Integer> countRanks(List<String> cards) {
        Map<String, Integer> map = new HashMap<>();
        for (String card : cards) {
            if (card == null || card.isBlank()) {
                throw new IllegalArgumentException("牌面不能为空");
            }
            String rank = cardRank(card.trim());
            map.merge(rank, 1, Integer::sum);
        }
        return map;
    }

    private static String cardRank(String card) {
        if ("BJ".equals(card) || "RJ".equals(card)) {
            return card;
        }
        // 10 的长度是 3，比如 S10、D10；其余长度 2
        return card.substring(1);
    }

    private static int weight(String rank) {
        int idx = RANK_ORDER.indexOf(rank);
        if (idx < 0) {
            throw new IllegalArgumentException("未知牌面:" + rank);
        }
        return idx;
    }

    private static boolean isStraight(Map<String, Integer> rankCount, int requiredCount) {
        if (rankCount.containsKey("2") || rankCount.containsKey("BJ") || rankCount.containsKey("RJ")) {
            return false;
        }
        if (rankCount.values().stream().anyMatch(v -> v != requiredCount)) {
            return false;
        }
        return isConsecutive(new ArrayList<>(rankCount.keySet()));
    }

    private static boolean isConsecutive(List<String> ranks) {
        if (ranks.size() < 2) {
            return false;
        }
        List<Integer> ws = new ArrayList<>();
        for (String rank : ranks) {
            if ("2".equals(rank) || "BJ".equals(rank) || "RJ".equals(rank)) {
                return false;
            }
            ws.add(weight(rank));
        }
        Collections.sort(ws);
        for (int i = 1; i < ws.size(); i++) {
            if (ws.get(i) - ws.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }

    private static List<String> ranksWithCount(Map<String, Integer> count, int target) {
        List<String> res = new ArrayList<>();
        for (Map.Entry<String, Integer> e : count.entrySet()) {
            if (e.getValue() == target) {
                res.add(e.getKey());
            }
        }
        return res;
    }

    private static int countPairs(Map<String, Integer> count) {
        int pairs = 0;
        for (Integer v : count.values()) {
            if (v == 2) {
                pairs++;
            }
        }
        return pairs;
    }

    private static int maxRank(Set<String> ranks) {
        int max = -1;
        for (String r : ranks) {
            max = Math.max(max, weight(r));
        }
        return max;
    }

    private static int maxRank(List<String> ranks) {
        int max = -1;
        for (String r : ranks) {
            max = Math.max(max, weight(r));
        }
        return max;
    }
}
