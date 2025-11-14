package com.xiarui.board_game_backend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiarui.board_game_backend.auth.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户资料相关Mapper.
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserAccount> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    UserAccount findByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在（排除当前用户）
     */
    @Select("SELECT COUNT(1) FROM users WHERE email = #{email} AND id <> #{userId}")
    boolean existsByEmailExcludingId(@Param("email") String email, @Param("userId") Long userId);
}
