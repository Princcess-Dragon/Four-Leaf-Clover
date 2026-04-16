package com.clover.controller;

import com.clover.result.Result;
import com.clover.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI智能体控制器
 * 提供自然语言对话和工具调用功能
 */
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    /**
     * 发送消息到AI助理
     * @param request 包含用户消息的请求
     * @return AI回复
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        try {
            String userId = request.getUserId();
            if (userId == null || userId.isEmpty()) {
                userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
            }

            String message = request.getMessage();
            if (message == null || message.isEmpty()) {
                return Result.error("消息不能为空");
            }

            log.info("收到聊天请求 [{}]: {}", userId, message);

            String response = agentService.handleMessage(userId, message);

            return Result.success(response);

        } catch (Exception e) {
            log.error("聊天失败", e);
            return Result.error("聊天失败：" + e.getMessage());
        }
    }

    /**
     * 清空对话历史
     */
    @PostMapping("/clear")
    public Result<String> clearHistory(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            agentService.clearHistory(userId);
            return Result.success("对话历史已清空");

        } catch (Exception e) {
            log.error("清空历史失败", e);
            return Result.error("清空历史失败：" + e.getMessage());
        }
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Result<List<Map<String, String>>> getHistory(@RequestParam String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            List<Map<String, String>> history = agentService.getHistory(userId);
            return Result.success(history);

        } catch (Exception e) {
            log.error("获取历史失败", e);
            return Result.error("获取历史失败：" + e.getMessage());
        }
    }

    /**
     * 获取最近的对话记录（用于右侧面板展示）
     */
    @GetMapping("/recent-conversations")
    public Result<List<ConversationSummary>> getRecentConversations(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                Long currentId = com.clover.context.BaseContext.getCurrentId();
                if (currentId != null) {
                    userId = String.valueOf(currentId);
                } else {
                    userId = "anonymous_user";
                }
            }

            List<ConversationSummary> conversations = agentService.getRecentConversations(userId, limit);
            return Result.success(conversations);

        } catch (Exception e) {
            log.error("获取最近对话失败", e);
            return Result.error("获取最近对话失败：" + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI助理服务运行正常");
    }

    /**
     * 聊天请求DTO
     */
    public static class ChatRequest {
        private String userId;
        private String message;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 对话摘要DTO（用于展示）
     */
    public static class ConversationSummary {
        private Long id;
        private String role;
        private String content;
        private String createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
