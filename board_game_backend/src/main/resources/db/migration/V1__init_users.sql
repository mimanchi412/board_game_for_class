-- V1__init_users.sql
-- 幂等创建 users 表
CREATE TABLE IF NOT EXISTS users (
                                     id               BIGSERIAL PRIMARY KEY,
                                     username         VARCHAR(32)  NOT NULL UNIQUE,
    password         VARCHAR(128) NOT NULL,
    nickname         VARCHAR(32),
    gender           SMALLINT     DEFAULT 0,          -- 0未知 1男 2女
    email            VARCHAR(64)  UNIQUE,
    role             VARCHAR(20)  DEFAULT 'USER',
    status           SMALLINT     DEFAULT 1,          -- 1正常 0封禁
    score            INT          DEFAULT 1000,
    win_count        INT          DEFAULT 0,
    lose_count       INT          DEFAULT 0,
    level            INT          DEFAULT 1,
    last_login_time  TIMESTAMP,
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    -- 头像字段（允许存储至5MB）
    avatar               BYTEA,
    avatar_content_type  VARCHAR(64),
    avatar_size          INT,
    avatar_sha256        CHAR(64),

    CONSTRAINT chk_avatar_size CHECK (avatar_size IS NULL OR avatar_size <= 5242880)
    );

-- 如果索引不存在则创建
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE tablename = 'users' AND indexname = 'idx_users_score'
    ) THEN
CREATE INDEX idx_users_score ON users(score);
END IF;
END $$;

-- 幂等创建更新时间触发器和函数
CREATE OR REPLACE FUNCTION set_update_time()
RETURNS TRIGGER AS $$
BEGIN
  NEW.update_time := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 如果存在旧触发器，先删除再重建
DROP TRIGGER IF EXISTS trg_users_set_update_time ON users;

CREATE TRIGGER trg_users_set_update_time
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION set_update_time();
