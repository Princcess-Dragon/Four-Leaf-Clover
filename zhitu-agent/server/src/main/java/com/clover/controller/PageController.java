package com.clover.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

/**
 * 页面控制器
 */
@Controller
public class PageController {

    /**
     * AI助理主页
     */
    @GetMapping("/index")
    public ResponseEntity<byte[]> agentPage() throws IOException {
        ClassPathResource resource = new ClassPathResource("pages/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource.getContentAsByteArray());
    }

    /**
     * 首页重定向到登录页
     */
    @GetMapping("/")
    public ResponseEntity<byte[]> index() throws IOException {
        ClassPathResource resource = new ClassPathResource("pages/login.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource.getContentAsByteArray());
    }

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public ResponseEntity<byte[]> loginPage() throws IOException {
        ClassPathResource resource = new ClassPathResource("pages/login.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource.getContentAsByteArray());
    }

    /**
     * 旅游规划页面
     */
    @GetMapping("/travel-planner")
    public ResponseEntity<byte[]> travelPlannerPage() throws IOException {
        ClassPathResource resource = new ClassPathResource("pages/travel-planner.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource.getContentAsByteArray());
    }

    /**
     * 地图测试页面
     */
    @GetMapping("/map-test")
    public ResponseEntity<byte[]> mapTestPage() throws IOException {
        ClassPathResource resource = new ClassPathResource("pages/map-test.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource.getContentAsByteArray());
    }

    /**
     * 处理 favicon.ico 请求，返回空响应避免错误日志
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
