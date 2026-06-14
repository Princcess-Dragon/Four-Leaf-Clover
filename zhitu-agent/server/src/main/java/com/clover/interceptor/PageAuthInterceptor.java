package com.clover.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 页面访问权限拦截器
 * 检查用户是否已登录（是否有token），未登录则重定向到登录页
 */
@Component
@Slf4j
public class PageAuthInterceptor implements HandlerInterceptor {

    /**
     * 在请求处理之前进行调用
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  被调用的处理器
     * @return true表示继续流程，false表示中断流程
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // 记录访问日志
        log.info("页面访问拦截: {}", uri);
        
        // 从Cookie或请求参数中获取token
        String token = getTokenFromRequest(request);
        
        // 如果没有token，重定向到登录页
        if (token == null || token.isEmpty()) {
            log.info("用户未登录，重定向到登录页: {}", uri);
            response.sendRedirect("/login");
            return false;
        }
        
        // 有token，允许访问
        log.info("用户已登录，允许访问: {}", uri);
        return true;
    }
    
    /**
     * 从请求中获取token
     * 优先从Cookie中获取，其次从请求参数中获取
     *
     * @param request HTTP请求
     * @return token字符串，如果不存在则返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. 尝试从Cookie中获取token
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // 2. 尝试从请求参数中获取token
        String token = request.getParameter("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        
        // 3. 尝试从请求头中获取token
        token = request.getHeader("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        
        return null;
    }
}
