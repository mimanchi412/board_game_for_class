package com.xiarui.board_game_backend.game.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 扑克牌工具类。
 */
public final class DeckUtil {

    private static final String[] SUITS = {"S", "H", "C", "D"};
    private static final String[] RANKS = {"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"};
    private static final Map<String, Integer> RANK_WEIGHT = new LinkedHashMap<>();

    static {
        int weight = 0;
        for (String rank : RANKS) {
            RANK_WEIGHT.put(rank, weight++);
        }
        RANK_WEIGHT.put("BJ", weight++);
        RANK_WEIGHT.put("RJ", weight);
    }

    private DeckUtil() {
    }

    /**
     * 生成并洗牌。
     */
    public static List<String> shuffledDeck() {
        List<String> cards = new ArrayList<>(54);
        for (String rank : RANKS) {
            for (String suit : SUITS) {
                cards.add(suit + rank);
            }
        }
        cards.add("BJ");
        cards.add("RJ");
        Collections.shuffle(cards);
        return cards;
    }

    /**
     * 根据斗地主规则权重排序，将大牌放在末尾方便前端展示。
     */
    public static void sortCards(List<String> cards) {
        cards.sort((a, b) -> Integer.compare(rankWeight(a), rankWeight(b)));
    }

    private static int rankWeight(String card) {
        if (card == null) {
            return -1;
        }
        if ("BJ".equals(card) || "RJ".equals(card)) {
            return RANK_WEIGHT.get(card);
        }
        String rank = card.substring(1);
        return RANK_WEIGHT.getOrDefault(rank, -1);
    }
}
