package com.clover.controller;

import com.clover.result.Result;
import com.clover.service.AgentService;
import com.clover.service.MapRouteService;
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

    @Autowired
    private MapRouteService mapRouteService;

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
                String currentId = com.clover.context.BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                } else {
                    userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
                }
            }

            String message = request.getMessage();
            if (message == null || message.isEmpty()) {
                return Result.error("消息不能为空");
            }

            log.info("收到聊天请求 [{}]: {}", userId, message);

            String response = agentService.handleMessage(userId, request.getSessionId(), message);

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
    public Result<List<Map<String, String>>> getHistory(
            @RequestParam String userId,
            @RequestParam(required = false) String sessionId) {
        try {
            log.info("收到获取历史请求 - userId: {}, sessionId: {}", userId, sessionId);
            
            if (userId == null || userId.isEmpty()) {
                log.warn("用户ID为空");
                return Result.error("用户ID不能为空");
            }

            List<Map<String, String>> history = agentService.getHistory(userId, sessionId);
            log.info("返回历史记录数量: {}", history != null ? history.size() : 0);
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
                String currentId = com.clover.context.BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
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
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<List<com.clover.mapper.ConversationMapper.SessionInfo>> getSessionList(
            @RequestParam(required = false) String userId) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = com.clover.context.BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                } else {
                    userId = "anonymous_user";
                }
            }

            List<com.clover.mapper.ConversationMapper.SessionInfo> sessions = agentService.getSessionList(userId);
            return Result.success(sessions);

        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Result.error("获取会话列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public Result<String> createSession(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String sessionTitle = request.getOrDefault("sessionTitle", "新对话");
            
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            String sessionId = agentService.createNewSession(userId, sessionTitle);
            return Result.success(sessionId);

        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Result.error("创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<String> deleteSession(
            @PathVariable String sessionId,
            @RequestParam(required = false) String userId) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = com.clover.context.BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                } else {
                    return Result.error("用户ID不能为空");
                }
            }

            agentService.deleteSession(userId, sessionId);
            return Result.success("会话已删除");

        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.error("删除会话失败：" + e.getMessage());
        }
    }

    /**
     * 切换会话
     */
    @PostMapping("/sessions/switch")
    public Result<String> switchSession(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String sessionId = request.get("sessionId");
            
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            
            if (sessionId == null || sessionId.isEmpty()) {
                return Result.error("会话ID不能为空");
            }

            agentService.switchSession(userId, sessionId);
            return Result.success("会话已切换");

        } catch (Exception e) {
            log.error("切换会话失败", e);
            return Result.error("切换会话失败：" + e.getMessage());
        }
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/sessions/{sessionId}/title")
    public Result<String> updateSessionTitle(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String sessionTitle = request.get("sessionTitle");
            
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            
            if (sessionTitle == null || sessionTitle.isEmpty()) {
                return Result.error("会话标题不能为空");
            }

            agentService.updateSessionTitle(userId, sessionId, sessionTitle);
            return Result.success("会话标题已更新");

        } catch (Exception e) {
            log.error("更新会话标题失败", e);
            return Result.error("更新会话标题失败：" + e.getMessage());
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
     * 获取驾车路线
     */
    @PostMapping("/route/driving")
    public Result<Map<String, Object>> getDrivingRoute(@RequestBody MapRouteRequest request) {
        try {
            if (request.getOrigin() == null || request.getDestination() == null) {
                return Result.error("起点和终点不能为空");
            }

            Map<String, Object> route = mapRouteService.getDrivingRoute(
                request.getOrigin(), 
                request.getDestination()
            );

            if (route.containsKey("error")) {
                return Result.error((String) route.get("message"));
            }

            return Result.success(route);

        } catch (Exception e) {
            log.error("获取驾车路线失败", e);
            return Result.error("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 获取步行路线
     */
    @PostMapping("/route/walking")
    public Result<Map<String, Object>> getWalkingRoute(@RequestBody MapRouteRequest request) {
        try {
            if (request.getOrigin() == null || request.getDestination() == null) {
                return Result.error("起点和终点不能为空");
            }

            Map<String, Object> route = mapRouteService.getWalkingRoute(
                request.getOrigin(), 
                request.getDestination()
            );

            if (route.containsKey("error")) {
                return Result.error((String) route.get("message"));
            }

            return Result.success(route);

        } catch (Exception e) {
            log.error("获取步行路线失败", e);
            return Result.error("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 获取公交路线
     */
    @PostMapping("/route/transit")
    public Result<Map<String, Object>> getTransitRoute(@RequestBody TransitRouteRequest request) {
        try {
            if (request.getOrigin() == null || request.getDestination() == null) {
                return Result.error("起点和终点不能为空");
            }

            String city = request.getCity() != null ? request.getCity() : "北京";

            Map<String, Object> route = mapRouteService.getTransitRoute(
                request.getOrigin(), 
                request.getDestination(),
                city
            );

            if (route.containsKey("error")) {
                return Result.error((String) route.get("message"));
            }

            return Result.success(route);

        } catch (Exception e) {
            log.error("获取公交路线失败", e);
            return Result.error("获取路线失败：" + e.getMessage());
        }
    }

    /**
     * 聊天请求DTO
     */
    public static class ChatRequest {
        private String userId;
        private String sessionId;
        private String message;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 地图路线请求DTO
     */
    public static class MapRouteRequest {
        private String origin;
        private String destination;

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }
    }

    /**
     * 公交路线请求DTO
     */
    public static class TransitRouteRequest {
        private String origin;
        private String destination;
        private String city;

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
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
