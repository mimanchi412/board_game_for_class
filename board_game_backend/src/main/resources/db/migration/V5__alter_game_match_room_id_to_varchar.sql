-- 将 game_match.room_id 改为可保存 UUID 的 varchar
ALTER TABLE game_match
    ALTER COLUMN room_id TYPE VARCHAR(64);
