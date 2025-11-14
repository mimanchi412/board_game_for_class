-- V3__add_game_and_stats_tables.sql

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_min_messages = WARNING;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_proc
    WHERE proname = 'tg_touch_update_time'
  ) THEN
    CREATE OR REPLACE FUNCTION tg_touch_update_time()
    RETURNS trigger AS $f$
BEGIN
      NEW.update_time = CURRENT_TIMESTAMP;
RETURN NEW;
END;
    $f$ LANGUAGE plpgsql;
END IF;
END$$;

-- =========================
-- 1) 用户汇总统计：user_stats
-- =========================
CREATE TABLE IF NOT EXISTS user_stats (
                                          user_id         BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_games     INT          NOT NULL DEFAULT 0,
    win_count       INT          NOT NULL DEFAULT 0,
    lose_count      INT          NOT NULL DEFAULT 0,
    score           INT          NOT NULL DEFAULT 1000,
    level           INT          NOT NULL DEFAULT 1,
    max_streak      INT          NOT NULL DEFAULT 0,
    current_streak  INT          NOT NULL DEFAULT 0,
    update_time     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stats_nonneg CHECK (
                                          total_games >= 0 AND win_count >= 0 AND lose_count >= 0 AND level >= 1
                                      )
    );

-- 常用查询索引（排行榜/胜率）
CREATE INDEX IF NOT EXISTS idx_user_stats_score ON user_stats(score);
CREATE INDEX IF NOT EXISTS idx_user_stats_win ON user_stats(win_count);

-- 触发器：自动更新时间
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'tr_user_stats_touch_update_time'
  ) THEN
CREATE TRIGGER tr_user_stats_touch_update_time
    BEFORE UPDATE ON user_stats
    FOR EACH ROW
    EXECUTE FUNCTION tg_touch_update_time();
END IF;
END$$;

COMMENT ON TABLE user_stats IS '用户长期累计统计：胜/负/积分/等级/连胜等';
COMMENT ON COLUMN user_stats.score IS '积分（可用 Elo/自定义规则）';

-- =========================
-- 2) 对局头表：game_match（每局一条）
-- =========================
CREATE TABLE IF NOT EXISTS game_match (
                                          id                BIGSERIAL PRIMARY KEY,
                                          room_id           BIGINT,
                                          landlord_user_id  BIGINT REFERENCES users(id),
    winner_side       VARCHAR(8) NOT NULL,  -- 'LANDLORD' / 'FARMER'
    start_time        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time          TIMESTAMPTZ,
    remark            TEXT,
    CONSTRAINT chk_winner_side CHECK (winner_side IN ('LANDLORD','FARMER'))
    );

CREATE INDEX IF NOT EXISTS idx_game_match_start_time ON game_match(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_game_match_room ON game_match(room_id);
CREATE INDEX IF NOT EXISTS idx_game_match_landlord ON game_match(landlord_user_id);

COMMENT ON TABLE game_match IS '对局头信息：房间、地主、胜负方、起止时间';

-- =========================
-- 3) 对局-玩家维度：game_match_player
-- =========================
CREATE TABLE IF NOT EXISTS game_match_player (
                                                 id            BIGSERIAL PRIMARY KEY,
                                                 match_id      BIGINT  NOT NULL REFERENCES game_match(id) ON DELETE CASCADE,
    user_id       BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seat          SMALLINT NOT NULL,             -- 0/1/2
    role          VARCHAR(8) NOT NULL,           -- 'LANDLORD'/'FARMER'
    result        VARCHAR(4) NOT NULL,           -- 'WIN'/'LOSE'
    score_delta   INT      NOT NULL DEFAULT 0,   -- 本局积分变化
    bombs         INT      NOT NULL DEFAULT 0,   -- 炸弹次数
    left_cards    SMALLINT  NOT NULL DEFAULT 0,  -- 结束时手牌数
    duration_sec  INT,
    CONSTRAINT uq_match_seat UNIQUE (match_id, seat),
    CONSTRAINT uq_match_user UNIQUE (match_id, user_id),
    CONSTRAINT chk_role CHECK (role IN ('LANDLORD','FARMER')),
    CONSTRAINT chk_result CHECK (result IN ('WIN','LOSE')),
    CONSTRAINT chk_seat CHECK (seat IN (0,1,2))
    );

-- 常用筛选：我的战绩列表、按时间倒序
CREATE INDEX IF NOT EXISTS idx_match_player_user ON game_match_player(user_id);
CREATE INDEX IF NOT EXISTS idx_match_player_match ON game_match_player(match_id);

COMMENT ON TABLE game_match_player IS '对局-玩家维度的结果，用于历史战绩和统计';
COMMENT ON COLUMN game_match_player.score_delta IS '本局积分增减';


CREATE TABLE IF NOT EXISTS game_move (
                                         id          BIGSERIAL PRIMARY KEY,
                                         match_id    BIGINT   NOT NULL REFERENCES game_match(id) ON DELETE CASCADE,
    step_no     INT      NOT NULL,               -- 全局步号 1..N
    player_id   BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seat        SMALLINT NOT NULL,               -- 0/1/2
    pattern     VARCHAR(20) NOT NULL,            -- 单张/对子/顺子/炸弹/王炸/飞机...
    cards_json  JSONB    NOT NULL,               -- 例：["3H","3D","3S"]
    beats_prev  BOOLEAN  NOT NULL,               -- 是否压过上一手
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_move_seat CHECK (seat IN (0,1,2))
    );

CREATE INDEX IF NOT EXISTS idx_move_match_step ON game_move(match_id, step_no);
-- 如果需要按牌内容检索，可开启 JSONB GIN（可选）
-- CREATE INDEX IF NOT EXISTS idx_move_cards_gin ON game_move USING GIN(cards_json);

COMMENT ON TABLE game_move IS '出牌动作流水，按 step_no 顺序用于回放';

-- =========================
-- 5) 数据回填：把 users 上的统计迁移到 user_stats（幂等 UPSERT）
-- 说明：你的 users 里已有 score / win_count / lose_count / level 字段
-- 这里把它们一次性写入 user_stats。之后代码侧读写 user_stats。
-- =========================
INSERT INTO user_stats(user_id, total_games, win_count, lose_count, score, level, update_time)
SELECT
    u.id AS user_id,
    COALESCE(u.win_count, 0) + COALESCE(u.lose_count, 0) AS total_games,
    COALESCE(u.win_count, 0) AS win_count,
    COALESCE(u.lose_count, 0) AS lose_count,
    COALESCE(u.score, 1000) AS score,
    COALESCE(u.level, 1) AS level,
    CURRENT_TIMESTAMP
FROM users u
    ON CONFLICT (user_id) DO UPDATE SET
    total_games = EXCLUDED.total_games,
                                 win_count   = EXCLUDED.win_count,
                                 lose_count  = EXCLUDED.lose_count,
                                 score       = EXCLUDED.score,
                                 level       = EXCLUDED.level,
                                 update_time = EXCLUDED.update_time;

-- =========================
-- 6) 视图（可选）：我的战绩列表的便捷视图
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_views WHERE viewname = 'v_my_matches'
  ) THEN
CREATE VIEW v_my_matches AS
SELECT
    gmp.user_id,
    gmp.match_id,
    gm.start_time,
    gm.end_time,
    gm.winner_side,
    gmp.role,
    gmp.result,
    gmp.score_delta,
    gmp.bombs,
    gmp.left_cards
FROM game_match_player gmp
         JOIN game_match gm ON gm.id = gmp.match_id;
END IF;
END$$;

COMMENT ON VIEW v_my_matches IS '用户维度的对局列表视图，便于接口按 user_id 查询历史战绩';
