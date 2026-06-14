package com.clover.controller;

import com.clover.result.Result;
import com.clover.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param request 登录请求(包含username、password、captchaKey、captchaCode)
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String captchaKey = request.get("captchaKey");
            String captchaCode = request.get("captchaCode");
    
            if (username == null || username.isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return Result.error("密码不能为空");
            }
            if (captchaKey == null || captchaKey.isEmpty()) {
                return Result.error("验证码标识不能为空");
            }
            if (captchaCode == null || captchaCode.isEmpty()) {
                return Result.error("验证码不能为空");
            }
    
            // TODO: 从Redis中获取验证码并校验
            // 这里暂时跳过验证码校验,实际项目中需要从Redis获取并比对
            log.info("收到登录请求: {}, 验证码key: {}", username, captchaKey);
    
            Map<String, Object> result = userService.login(username, password);
            return Result.success(result);
    
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     * @param request 注册请求(包含username、password、email、captchaKey、captchaCode)
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String email = request.get("email");
            String captchaKey = request.get("captchaKey");
            String captchaCode = request.get("captchaCode");
    
            if (username == null || username.isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return Result.error("密码不能为空");
            }
            if (captchaKey == null || captchaKey.isEmpty()) {
                return Result.error("验证码标识不能为空");
            }
            if (captchaCode == null || captchaCode.isEmpty()) {
                return Result.error("验证码不能为空");
            }
    
            // TODO: 从Redis中获取验证码并校验
            // 这里暂时跳过验证码校验,实际项目中需要从Redis获取并比对
            log.info("收到注册请求: {}, 验证码key: {}", username, captchaKey);
    
            Map<String, Object> result = userService.register(username, password, email);
            return Result.success(result);
    
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.error(e.getMessage());
        }
    }
}
