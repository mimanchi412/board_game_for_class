package com.xiarui.board_game_backend.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 用户账户实体，对应 users 表。
 * 包含基础信息、扩展资料、统计数据以及头像元数据等字段。
 */
@Data
@TableName("users")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 加密后的登录密码 */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 绑定邮箱 */
    private String email;

    /** 角色（USER / ADMIN） */
    private String role;

    /** 最近一次登录时间 */
    private OffsetDateTime lastLoginTime;

    /** 个性签名/简介 */
    private String bio;

    /** 生日 */
    private LocalDate birthday;

    /** 所在地区 */
    private String region;

    /** 乐观锁版本号 */
    private Integer version;

    /** 状态：1正常 0封禁 */
    private Integer status;

    /** 等级积分 */
    private Integer score;

    /** 获胜场次 */
    private Integer winCount;

    /** 失败场次 */
    private Integer loseCount;

    /** 等级 */
    private Integer level;

    /** 头像二进制数据 */
    private byte[] avatar;

    /** 头像文件类型 */
    private String avatarContentType;

    /** 头像大小（字节） */
    private Integer avatarSize;

    /** 头像内容的 SHA-256 指纹 */
    private String avatarSha256;
}
