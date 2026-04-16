package com.clover.service;

import com.clover.context.BaseContext;
import com.clover.entity.Conversation;
import com.clover.mapper.ConversationMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI办公助理核心服务
 * 能理解自然语言并执行各种操作
 */
@Slf4j
@Service
public class AgentService {

    @Value("${agent.api.key:}")
    private String apiKey;

    @Value("${agent.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${agent.api.model:gpt-3.5-turbo}")
    private String apiModel;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FileService fileService;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CalculatorService calculatorService;

    @Autowired
    private ConversationMapper conversationMapper;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 对话历史（内存缓存）
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    
    // 会话ID映射（userId -> sessionId）
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    // 系统提示词
    private static final String SYSTEM_PROMPT = 
        "你是一个能说的AI办公助理，你可以：\n" +
        "1. 管理任务（创建、查询、删除、完成任务）\n" +
        "2. 文件操作（创建、读取、更新文件）\n" +
        "3. 查询天气信息\n" +
        "4. 执行数学计算\n" +
        "5. 回答一般问题\n\n" +
        "请用简洁友好的中文回复。当用户要求执行操作时，先确认理解，然后调用相应工具执行。";

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
            .baseUrl(apiUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
            .build();
        
        log.info("通义千问API初始化完成: url={}, model={}", apiUrl, apiModel);
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your-")) {
            log.warn("⚠️ 警告: API Key 未正确配置，请在 application-dev.yml 中配置有效的通义千问 API Key");
        }
    }

    /**
     * 处理用户消息
     * @param userId 用户ID（可选，如果不传则从登录上下文获取）
     * @param message 用户消息
     * @return AI回复
     */
    public String handleMessage(String userId, String message) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                Long currentId = BaseContext.getCurrentId();
                if (currentId != null) {
                    userId = String.valueOf(currentId);
                } else {
                    userId = "anonymous_user";
                }
            }
            
            log.info("收到用户消息 [{}]: {}", userId, message);
            
            // 获取或创建会话ID
            String sessionId = getOrCreateSessionId(userId);

            // 获取或初始化对话历史
            List<Map<String, String>> history = conversationHistory.computeIfAbsent(userId, 
                k -> {
                    List<Map<String, String>> list = new ArrayList<>();
                    Map<String, String> systemMsg = new HashMap<>();
                    systemMsg.put("role", "system");
                    systemMsg.put("content", SYSTEM_PROMPT);
                    list.add(systemMsg);
                    return list;
                });

            // 添加用户消息到历史
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            history.add(userMsg);
            
            // 保存用户消息到数据库
            saveConversationToDB(userId, sessionId, "user", message, "text", null);

            // 调用AI API获取回复
            String aiResponse = callAIAPIService(history);

            // 检查是否需要执行工具
            String toolResult = checkAndExecuteTools(message);
            
            String finalResponse = aiResponse;
            if (toolResult != null && !toolResult.isEmpty()) {
                finalResponse = aiResponse + "\n\n" + toolResult;
            }

            // 添加AI回复到历史
            Map<String, String> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", finalResponse);
            history.add(assistantMsg);
            
            // 保存AI回复到数据库
            saveConversationToDB(userId, sessionId, "assistant", finalResponse, "text", null);

            // 限制历史长度
            if (history.size() > 20) {
                history.subList(1, 3).clear(); // 保留system prompt和最近的历史
            }

            log.info("AI回复 [{}]: {}", userId, finalResponse);
            return finalResponse;

        } catch (Exception e) {
            log.error("处理消息失败", e);
            return "抱歉，我遇到了一些问题，请稍后再试。错误信息：" + e.getMessage();
        }
    }

    /**
     * 调用AI API服务（通义千问）
     */
    private String callAIAPIService(List<Map<String, String>> messages) {
        try {
            // 检查 API Key 是否有效
            if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your-")) {
                log.error("API Key 未配置或无效: {}", apiKey);
                return "⚠️ API Key 未配置！\n\n请按以下步骤配置：\n" +
                       "1. 访问 https://dashscope.console.aliyun.com/\n" +
                       "2. 获取你的 API Key\n" +
                       "3. 在 application-dev.yml 中配置 agent.api.key 字段\n" +
                       "4. 重启应用";
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", apiModel);
            
            // 通义千问使用 input.messages 格式
            Map<String, Object> input = new HashMap<>();
            input.put("messages", messages);
            requestBody.put("input", input);
            
            // 可选参数
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            parameters.put("max_tokens", 1000);
            parameters.put("temperature", 0.7);
            requestBody.put("parameters", parameters);

            log.info("调用通义千问API: model={}, messages={}", apiModel, messages.size());

            // 添加重试机制：最多重试2次，每次间隔1秒
            String response = null;
            int maxRetries = 2;
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    log.debug("第 {} 次尝试调用通义千问API...", attempt);
                    response = webClient.post()
                        .uri(apiUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .header("X-DashScope-SSE", "disable")
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("通义千问API客户端错误 [{}]: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("API认证失败: " + clientResponse.statusCode()));
                                });
                        })
                        .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("通义千问API服务器错误 [{}]: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("服务器错误: " + clientResponse.statusCode()));
                                });
                        })
                        .bodyToMono(String.class)
                        .block(java.time.Duration.ofSeconds(60)); // 设置60秒超时
                    
                    // 成功则跳出循环
                    if (response != null) {
                        log.debug("第 {} 次尝试成功", attempt);
                        break;
                    }
                } catch (Exception e) {
                    lastException = e;
                    log.warn("第 {} 次尝试失败: {}", attempt, e.getMessage());
                    
                    // 如果不是最后一次尝试，等待后重试
                    if (attempt < maxRetries) {
                        try {
                            log.info("等待1秒后重试...");
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("重试被中断", ie);
                        }
                    }
                }
            }
            
            // 如果所有重试都失败
            if (response == null) {
                throw new RuntimeException("经过 " + maxRetries + " 次重试后仍然失败", lastException);
            }

            log.debug("通义千问API响应: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查是否有错误
            if (jsonNode.has("code")) {
                String errorCode = jsonNode.path("code").asText();
                String errorMessage = jsonNode.path("message").asText();
                log.error("通义千问API错误 [{}]: {}", errorCode, errorMessage);
                
                // 提供更友好的错误提示
                if ("InvalidApiKey".equals(errorCode)) {
                    return "❌ API Key 无效！\n请检查：\n" +
                           "1. API Key 是否正确复制\n" +
                           "2. API Key 是否已激活\n" +
                           "3. 账户余额是否充足";
                } else if ("ServiceUnavailable".equals(errorCode)) {
                    return "抱歉，通义千问服务暂时不可用，请稍后重试。";
                } else if ("RateLimitExceeded".equals(errorCode)) {
                    return "请求频率超限，请稍后再试。";
                }
                
                return "抱歉，AI服务暂时不可用：" + errorMessage;
            }
            
            // 解析通义千问的响应格式: output.choices[0].message.content
            if (!jsonNode.has("output")) {
                log.error("响应格式异常: {}", response);
                return "抱歉，AI服务返回了意外格式的响应。";
            }
            
            String content = jsonNode.path("output")
                                    .path("choices")
                                    .get(0)
                                    .path("message")
                                    .path("content")
                                    .asText();
            
            if (content.isEmpty()) {
                log.warn("通义千问返回空响应");
                return "抱歉，我暂时无法回答这个问题。";
            }
            
            return content;

        } catch (Exception e) {
            log.error("调用通义千问API失败", e);
            
            // 根据异常类型提供更具体的错误信息
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "❌ 认证失败（401）\n\n可能原因：\n" +
                       "1. API Key 错误或未配置\n" +
                       "2. API Key 格式不正确\n" +
                       "3. API Key 已过期或被禁用\n\n" +
                       "请检查 application-dev.yml 中的 agent.api.key 配置。";
            } else if (e.getMessage() != null && (e.getMessage().contains("Connection reset") || 
                                                   e.getMessage().contains("timeout") ||
                                                   e.getCause() instanceof java.net.SocketException)) {
                return "⚠️ 网络连接问题\n\n可能原因：\n" +
                       "1. 网络连接不稳定\n" +
                       "2. 防火墙阻止了请求\n" +
                       "3. API服务暂时不可用\n\n" +
                       "建议：\n" +
                       "• 检查网络连接\n" +
                       "• 稍后重试\n" +
                       "• 如使用代理，请确认代理配置正确";
            }
            
            return "我理解你的需求，但暂时无法连接到AI服务。\n错误详情：" + e.getMessage();
        }
    }

    /**
     * 检查并执行工具调用
     */
    private String checkAndExecuteTools(String message) {
        String lowerMessage = message.toLowerCase();
        StringBuilder result = new StringBuilder();

        // 任务管理
        if (lowerMessage.contains("任务") || lowerMessage.contains("待办") || lowerMessage.contains("todo")) {
            result.append(taskService.handleTaskRequest(message));
        }
        
        // 文件操作
        else if (lowerMessage.contains("文件") || lowerMessage.contains("创建") || lowerMessage.contains("保存")) {
            result.append(fileService.handleFileRequest(message));
        }
        
        // 天气查询
        else if (lowerMessage.contains("天气")) {
            result.append(weatherService.handleWeatherRequest(message));
        }
        
        // 数学计算
        else if (lowerMessage.contains("计算") || lowerMessage.contains("等于") || 
                 lowerMessage.matches(".*\\d+\\s*[+\\-*/].*")) {
            result.append(calculatorService.handleCalculationRequest(message));
        }

        return result.length() > 0 ? result.toString() : null;
    }

    /**
     * 清空对话历史
     */
    public void clearHistory(String userId) {
        // 如果 userId 为空，尝试从登录上下文获取
        if (userId == null || userId.isEmpty()) {
            Long currentId = BaseContext.getCurrentId();
            if (currentId != null) {
                userId = String.valueOf(currentId);
            }
        }
        
        conversationHistory.remove(userId);
        sessionMap.remove(userId);
        
        // 删除数据库中的对话记录
        if (userId != null && !userId.equals("anonymous_user")) {
            conversationMapper.deleteByUserId(userId);
            log.info("已清空用户 [{}] 的对话历史", userId);
        }
    }

    /**
     * 获取对话历史
     */
    public List<Map<String, String>> getHistory(String userId) {
        return conversationHistory.getOrDefault(userId, new ArrayList<>());
    }
    
    /**
     * 获取最近的对话记录（从数据库）
     */
    public List<com.clover.controller.AgentController.ConversationSummary> getRecentConversations(String userId, int limit) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                Long currentId = BaseContext.getCurrentId();
                if (currentId != null) {
                    userId = String.valueOf(currentId);
                }
            }
            
            List<Conversation> conversations = conversationMapper.findRecentByUserId(userId, limit);
            List<com.clover.controller.AgentController.ConversationSummary> summaries = new ArrayList<>();
            
            for (Conversation conv : conversations) {
                com.clover.controller.AgentController.ConversationSummary summary = 
                    new com.clover.controller.AgentController.ConversationSummary();
                summary.setId(conv.getId());
                summary.setRole(conv.getRole());
                summary.setContent(conv.getContent());
                summary.setCreatedAt(conv.getCreatedAt() != null ? 
                    conv.getCreatedAt().toString() : "");
                summaries.add(summary);
            }
            
            // 按时间正序排列（最早的在前）
            Collections.reverse(summaries);
            
            return summaries;
        } catch (Exception e) {
            log.error("获取最近对话失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取或创建会话ID
     */
    private String getOrCreateSessionId(String userId) {
        return sessionMap.computeIfAbsent(userId, k -> {
            String sessionId = "session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("为用户 [{}] 创建新会话: {}", userId, sessionId);
            return sessionId;
        });
    }
    
    /**
     * 保存对话到数据库
     */
    private void saveConversationToDB(String userId, String sessionId, String role, 
                                      String content, String messageType, String toolCalls) {
        try {
            Conversation conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setSessionId(sessionId);
            conversation.setRole(role);
            conversation.setContent(content);
            conversation.setMessageType(messageType != null ? messageType : "text");
            conversation.setToolCalls(toolCalls);
            conversation.setCreatedAt(LocalDateTime.now());
            
            conversationMapper.insert(conversation);
            log.debug("对话已保存到数据库: userId={}, sessionId={}, role={}", userId, sessionId, role);
        } catch (Exception e) {
            log.error("保存对话到数据库失败", e);
            // 不抛出异常，避免影响正常聊天流程
        }
    }
}
