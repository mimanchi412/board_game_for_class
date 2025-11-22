-- 为 game_match_player 增加逃跑标记
ALTER TABLE game_match_player
    ADD COLUMN IF NOT EXISTS escaped BOOLEAN NOT NULL DEFAULT FALSE;
