package com.xiarui.board_game_backend.common.constants;

/**
 * Redis常量类
 */
public class RedisConstants {

    /** 默认过期时间（秒） */
    public static final long DEFAULT_EXPIRE_TIME = 60 * 30; // 30分钟

    /** 验证码过期时间（秒） */
    public static final long VERIFICATION_CODE_EXPIRE_TIME = 60 * 5; // 5分钟

    /** 用户信息缓存过期时间（秒） */
    public static final long USER_INFO_EXPIRE_TIME = 60 * 60 * 24; // 24小时

    /** 用户Token缓存过期时间（秒） */
    public static final long USER_TOKEN_EXPIRE_TIME = 60 * 60 * 24; // 24小时

    /** JWT黑名单过期时间（秒） */
    public static final long JWT_BLACKLIST_EXPIRE_TIME = 60 * 60 * 24 * 7; // 7天

    // ==================== 键名前缀 ====================

    /** 验证码前缀 */
    public static final String VERIFICATION_CODE_PREFIX = "verification_code:";

    /** 用户信息前缀 */
    public static final String USER_INFO_PREFIX = "user_info:";

    /** 用户权限前缀 */
    public static final String USER_PERMISSION_PREFIX = "user_permission:";

    /** 用户角色前缀 */
    public static final String USER_ROLE_PREFIX = "user_role:";

    /** JWT黑名单前缀 */
    public static final String JWT_BLACKLIST_PREFIX = "jwt_blacklist:";

    /** 用户Token前缀 */
    public static final String USER_TOKEN_PREFIX = "user_token:";

    /** 登录失败次数前缀 */
    public static final String LOGIN_FAIL_COUNT_PREFIX = "login_fail_count:";

    /** 登录锁定前缀 */
    public static final String LOGIN_LOCK_PREFIX = "login_lock:";

    /** 邮箱验证码发送频率限制前缀 */
    public static final String EMAIL_CODE_RATE_LIMIT_PREFIX = "email_code_rate_limit:";
    /** 邮箱验证码请求间隔（秒） */
    public static final long EMAIL_CODE_RATE_LIMIT_SECONDS = 60L;

    // ==================== 匹配/房间相关 ====================

    /** 随机匹配池集合 key */
    public static final String GAME_MATCH_POOL_KEY = "game:match:pool";

    /** 匹配队列分布式锁 key */
    public static final String GAME_MATCH_LOCK_KEY = "game:match:lock";

    /** 匹配 ticket key 前缀（控制单人等待超时） */
    public static final String GAME_MATCH_TICKET_PREFIX = "game:match:ticket:";

    /** 匹配等待时长（秒） */
    public static final long GAME_MATCH_WAIT_SECONDS = 300L;

    /** 房间信息 key 前缀 */
    public static final String GAME_ROOM_INFO_PREFIX = "game:room:info:";

    /** 房间码 -> 房间ID 的映射 key 前缀 */
    public static final String GAME_ROOM_CODE_PREFIX = "game:room:code:";

    /** 房间信息 TTL（秒） */
    public static final long GAME_ROOM_TTL_SECONDS = 600L;

    /** 房间创建限流 key 前缀 */
    public static final String GAME_ROOM_CREATE_LIMIT_PREFIX = "game:room:create:limit:";

    /** 每个用户一分钟内最多创建 5 次 */
    public static final int GAME_ROOM_CREATE_LIMIT_MAX = 5;

    /** 限流窗口（秒） */
    public static final long GAME_ROOM_CREATE_LIMIT_WINDOW_SECONDS = 60L;

    /** 房间开始锁前缀 */
    public static final String GAME_ROOM_START_LOCK_PREFIX = "game:room:start:lock:";

    /** 用户 -> 房间 映射前缀 */
    public static final String GAME_ROOM_USER_PREFIX = "game:room:user:";

    /** 短信验证码发送频率限制前缀 */
    public static final String SMS_CODE_RATE_LIMIT_PREFIX = "sms_code_rate_limit:";

    // ==================== 特定键名 ====================

    /** 系统配置键 */
    public static final String SYSTEM_CONFIG_KEY = "system:config";

    /** 在线用户集合 */
    public static final String ONLINE_USERS_KEY = "online:users";

    /** 系统访问计数器 */
    public static final String SYSTEM_VISIT_COUNT_KEY = "system:visit:count";

    /** 每日访问计数器前缀 */
    public static final String DAILY_VISIT_COUNT_PREFIX = "daily:visit:count:";

    // ==================== 方法 ====================

    /**
     * 获取验证码键名
     * @param identifier 标识（邮箱或手机号）
     * @return 键名
     */
    public static String getVerificationCodeKey(String identifier) {
        return VERIFICATION_CODE_PREFIX + identifier;
    }

    /**
     * 获取用户信息键名
     * @param userId 用户ID
     * @return 键名
     */
    public static String getUserInfoKey(String userId) {
        return USER_INFO_PREFIX + userId;
    }

    /**
     * 获取用户权限键名
     * @param userId 用户ID
     * @return 键名
     */
    public static String getUserPermissionKey(String userId) {
        return USER_PERMISSION_PREFIX + userId;
    }

    /**
     * 获取用户角色键名
     * @param userId 用户ID
     * @return 键名
     */
    public static String getUserRoleKey(String userId) {
        return USER_ROLE_PREFIX + userId;
    }

    /**
     * 获取JWT黑名单键名
     * @param token JWT令牌
     * @return 键名
     */
    public static String getJwtBlacklistKey(String token) {
        return JWT_BLACKLIST_PREFIX + token;
    }

    /**
     * 获取登录失败次数键名
     * @param identifier 标识（用户名或IP）
     * @return 键名
     */
    public static String getLoginFailCountKey(String identifier) {
        return LOGIN_FAIL_COUNT_PREFIX + identifier;
    }

    /**
     * 获取登录锁定键名
     * @param identifier 标识（用户名或IP）
     * @return 键名
     */
    public static String getLoginLockKey(String identifier) {
        return LOGIN_LOCK_PREFIX + identifier;
    }

    /**
     * 获取邮箱验证码发送频率限制键名
     * @param email 邮箱
     * @return 键名
     */
    public static String getEmailCodeRateLimitKey(String email) {
        return EMAIL_CODE_RATE_LIMIT_PREFIX + email;
    }

    /**
     * 获取短信验证码发送频率限制键名
     * @param phone 手机号
     * @return 键名
     */
    public static String getSmsCodeRateLimitKey(String phone) {
        return SMS_CODE_RATE_LIMIT_PREFIX + phone;
    }

    /**
     * 获取每日访问计数器键名
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 键名
     */
    public static String getDailyVisitCountKey(String date) {
        return DAILY_VISIT_COUNT_PREFIX + date;
    }
}
