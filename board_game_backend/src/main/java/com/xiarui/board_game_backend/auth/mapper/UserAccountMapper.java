package com.xiarui.board_game_backend.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiarui.board_game_backend.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户账户Mapper接口
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户账户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    UserAccount findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户账户信息
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    UserAccount findByEmail(@Param("email") String email);

    /**
     * 根据用户名或邮箱查询用户
     *
     * @param usernameOrEmail 用户名或邮箱
     * @return 用户账户信息
     */
    @Select("SELECT * FROM users WHERE username = #{usernameOrEmail} OR email = #{usernameOrEmail}")
    UserAccount findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    @Select("SELECT COUNT(1) FROM users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 存在返回true，否则返回false
     */
    @Select("SELECT COUNT(1) FROM users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);
}