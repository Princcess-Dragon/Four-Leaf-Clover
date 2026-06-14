package com.clover.service;

import com.clover.entity.User;
import com.clover.mapper.UserMapper;
import com.clover.properties.JwtProperties;
import com.clover.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务类
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果（包含token）
     */
    public Map<String, Object> login(String username, String password) {
        log.info("用户登录尝试: {}", username);

        // 1. 查询用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 验证密码
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }

        // 4. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        
        String token = JwtUtil.createJWT(
            jwtProperties.getUserSecretKey(),
            jwtProperties.getUserTtl(),
            claims
        );

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        
        log.info("用户登录成功: {}", username);
        return result;
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @return 注册结果
     */
    public Map<String, Object> register(String username, String password, String email) {
        log.info("用户注册尝试: {}", username);

        // 1. 检查用户名是否已存在
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 创建新用户
        User newUser = new User();
        newUser.setUserId("user_" + UUID.randomUUID().toString().substring(0, 8));
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.setStatus(1);

        // 3. 插入数据库
        userMapper.insert(newUser);

        // 4. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", newUser.getUserId());
        claims.put("username", newUser.getUsername());
        
        String token = JwtUtil.createJWT(
            jwtProperties.getUserSecretKey(),
            jwtProperties.getUserTtl(),
            claims
        );

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", newUser.getUserId());
        result.put("username", newUser.getUsername());
        
        log.info("用户注册成功: {}", username);
        return result;
    }
}
