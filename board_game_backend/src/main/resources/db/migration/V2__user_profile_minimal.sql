-- V2__user_profile_minimal.sql
-- 在现有 users 表上做最小化的“个人资料”扩展

-- 1) 新增最小资料字段（如果不存在）
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS bio       VARCHAR(160),
    ADD COLUMN IF NOT EXISTS birthday  DATE,
    ADD COLUMN IF NOT EXISTS region    VARCHAR(64),
    ADD COLUMN IF NOT EXISTS version   INT NOT NULL DEFAULT 0; -- 乐观锁

-- 2) 时间列升级为带时区（若原本为 timestamp without time zone）
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='users' AND column_name='create_time'
               AND data_type='timestamp without time zone') THEN
ALTER TABLE users
ALTER COLUMN create_time TYPE TIMESTAMPTZ USING create_time AT TIME ZONE 'UTC';
END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='users' AND column_name='update_time'
               AND data_type='timestamp without time zone') THEN
ALTER TABLE users
ALTER COLUMN update_time TYPE TIMESTAMPTZ USING update_time AT TIME ZONE 'UTC';
END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='users' AND column_name='last_login_time'
               AND data_type='timestamp without time zone') THEN
ALTER TABLE users
ALTER COLUMN last_login_time TYPE TIMESTAMPTZ USING last_login_time AT TIME ZONE 'UTC';
END IF;
END $$;

-- 3) 约束补强（性别/分数/胜负/等级/头像MIME白名单，保留你已有的头像大小检查）
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_users_gender') THEN
ALTER TABLE users ADD CONSTRAINT chk_users_gender CHECK (gender IN (0,1,2));
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_users_score_nonneg') THEN
ALTER TABLE users ADD CONSTRAINT chk_users_score_nonneg CHECK (score >= 0);
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_users_win_lose_nonneg') THEN
ALTER TABLE users ADD CONSTRAINT chk_users_win_lose_nonneg CHECK (win_count >= 0 AND lose_count >= 0);
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_users_level_min') THEN
ALTER TABLE users ADD CONSTRAINT chk_users_level_min CHECK (level >= 1);
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_users_avatar_mime') THEN
ALTER TABLE users ADD CONSTRAINT chk_users_avatar_mime
    CHECK (avatar_content_type IS NULL OR avatar_content_type IN ('image/png','image/jpeg','image/webp'));
END IF;
END $$;

-- 4) 索引（大小写不敏感 email / username）
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename='users' AND indexname='ux_users_email_lower') THEN
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email)) WHERE email IS NOT NULL;
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename='users' AND indexname='ux_users_username_lower') THEN
CREATE UNIQUE INDEX ux_users_username_lower ON users (lower(username));
END IF;
END $$;

