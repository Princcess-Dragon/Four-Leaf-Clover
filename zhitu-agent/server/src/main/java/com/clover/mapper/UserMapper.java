package com.clover.mapper;

import com.clover.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE user_id = #{userId}")
    User findByUserId(String userId);

    /**
     * 插入新用户
     * @param user 用户信息
     * @return 影响行数
     */
    @Insert("INSERT INTO user (user_id, username, password, email, phone, avatar, status) " +
            "VALUES (#{userId}, #{username}, #{password}, #{email}, #{phone}, #{avatar}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 影响行数
     */
    @Update("UPDATE user SET username=#{username}, password=#{password}, email=#{email}, " +
            "phone=#{phone}, avatar=#{avatar}, status=#{status}, updated_at=NOW() " +
            "WHERE user_id=#{userId}")
    int update(User user);
}
