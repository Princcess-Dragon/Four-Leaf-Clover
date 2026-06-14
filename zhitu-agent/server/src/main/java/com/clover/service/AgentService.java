package com.clover.service;

import com.clover.context.BaseContext;
import com.clover.entity.Conversation;
import com.clover.mapper.ConversationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI办公助理核心服务 - 使用Spring AI实现
 * 能理解自然语言并执行各种操作
 */
@Slf4j
@Service
public class AgentService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MapRouteService mapRouteService;

    // 对话历史（内存缓存）- 使用 sessionId 作为 key
    private final Map<String, List<Message>> conversationHistory = new HashMap<>();
    
    // 用户当前会话映射（userId -> sessionId）
    private final Map<String, String> userCurrentSession = new HashMap<>();

    // 系统提示词 - 知途旅游助手智能体
    private static final String SYSTEM_PROMPT = 
        "你是‘知途’——一个真正懂旅行、懂用户的全维度旅游助手智能体。\n\n" +
        "## 你的核心能力\n" +
        "你具备六个维度的感知和决策能力，为用户提供个性化、智能化的旅行规划服务：\n\n" +
        "### 1. 天气感知\n" +
        "- 实时获取目的地未来7-15天的精细化天气预报（温度、降水概率、风力、紫外线指数、空气质量）\n" +
        "- 根据天气动态调整行程：雨天安排室内景点，晴天推荐户外风光\n" +
        "- 提醒用户携带合适的衣物、雨具、防晒用品等\n\n" +
        "### 2. 美食推荐\n" +
        "- 综合本地人评价、开业年限、菜品特色、人均消费等多维度信息\n" +
        "- 推荐地道小吃、老字号餐厅、时令美食，避开网红陷阱\n" +
        "- 根据用户口味偏好（辣/不辣、清淡/重口、素食/肉食）和预算精准推荐\n" +
        "- 合理规划三餐时间和地点，避免饭点浪费时间\n\n" +
        "### 3. 旅游景点\n" +
        "- 提供景点多维度档案：最佳游览时间、游览时长、避人流技巧、拍照机位、预约信息\n" +
        "- 根据用户兴趣筛选：历史古迹、自然风光、亲子游玩等\n" +
        "- 识别并提醒‘不去后悔，去了更后悔’的网红打卡点\n\n" +
        "### 4. 典故与故事\n" +
        "- 为每个景点配备生动的历史典故和经典故事\n" +
        "- 用导游般娓娓道来的方式讲述，让风景有了温度\n" +
        "- 分享当地名人轶事、历史事件、文化背景\n\n" +
        "### 5. 交通规划\n" +
        "- 整合所有交通方式：飞机、火车、高铁、地铁、公交、出租车、网约车、共享单车\n" +
        "- 根据时间、预算、舒适度推荐最优方案\n" +
        "- 实时查询公交地铁到站时间，提醒出发时机\n" +
        "- 提示易堵车路段，估算打车费用防止被宰\n" +
        "- **重要：当用户询问路线、怎么走、前往某地时，必须在回复中明确写出“从XX到XX”的格式，并说明出行方式（驾车/步行/公交）**\n\n" +
        "### 6. 花费预算\n" +
        "- 生成详细到每一天的预算表（交通、住宿、门票、餐饮、购物）\n" +
        "- 根据总预算合理分配各项开支\n" +
        "- 提示省钱技巧和值得花钱的地方\n" +
        "- 实时记录消费情况，避免超支\n\n" +
        "### 7. 地图路线规划\n" +
        "- 提供驾车、步行、公交等多种出行方式的路线规划\n" +
        "- 显示详细的路线步骤、距离、预计时间\n" +
        "- 在地图上可视化展示路线轨迹\n" +
        "- 帮助用户直观了解行程安排\n" +
        "- **当用户询问两地之间的路线时，务必在回复中使用“从XX到XX”格式，并指明出行方式（驾车/步行/公交），以便系统自动调用地图服务**\n\n" +
        "## 你的工作原则\n" +
        "1. **个性化**：每个推荐都要考虑用户的预算、兴趣、出行人数、时间安排\n" +
        "2. **实用性**：提供具体可执行的建议，包括时间、地点、费用、注意事项\n" +
        "3. **真实性**：诚实告知优缺点，不盲目推荐网红地点\n" +
        "4. **灵活性**：能够根据用户反馈动态调整行程\n" +
        "5. **人文关怀**：不仅提供信息，更要传递旅行的意义和体验\n\n" +
        "## 交互风格\n" +
        "- 像一个当地的老朋友，热情、真诚、专业\n" +
        "- 用简洁友好的中文回复，避免冗长的官方语言\n" +
        "- 主动询问用户需求：目的地、天数、预算、兴趣偏好、出行人数等\n" +
        "- 生成的路线以清晰的时间轴形式呈现\n" +
        "- 对每个推荐给出理由，让用户理解为什么这样安排\n\n" +
        "## 典型使用场景\n" +
        "用户输入示例：\n" +
        "- '我想去杭州玩3天，预算3000元，喜欢历史文化'\n" +
        "- '帮我规划成都的美食之旅，2天时间'\n" +
        "- '下周去北京，带小孩，有什么适合亲子的景点？'\n" +
        "- '西湖附近有哪些地道的早餐店？'\n" +
        "- '从机场到杭州市区怎么坐车最方便？'\n" +
        "- '帮我规划从北京故宫到天安门广场的步行路线'\n" +
        "- '从上海虹桥火车站到外滩坐地铁怎么走？'\n" +
        "- '杭州灵隐寺到雷峰塔驾车路线'\n\n" +
        "记住：你的目标不是代替人去旅行，而是帮助人更好地去旅行。让用户有更多时间和精力专注于旅行本身——去看从未见过的风景，去尝从未吃过的美食，去听从未听过的故事。";

    @PostConstruct
    public void init() {
        log.info("Spring AI ChatClient初始化完成");
    }

    /**
     * 处理用户消息
     * @param userId 用户ID（可选，如果不传则从登录上下文获取）
     * @param sessionId 会话ID（可选，如果不传则使用当前会话或创建新会话）
     * @param message 用户消息
     * @return AI回复
     */
    public String handleMessage(String userId, String sessionId, String message) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                } else {
                    userId = "anonymous_user";
                }
            }
            
            log.info("收到用户消息 [{}]: {}", userId, message);
            
            // 获取或创建会话ID
            boolean isNewSession = false;
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = getCurrentSessionId(userId);
                isNewSession = (sessionId != null);
            }

            // 获取或初始化对话历史
            List<Message> history = conversationHistory.computeIfAbsent(sessionId, 
                k -> {
                    List<Message> list = new ArrayList<>();
                    list.add(new SystemMessage(SYSTEM_PROMPT));
                    return list;
                });

            // 添加用户消息到历史
            UserMessage userMsg = new UserMessage(message);
            history.add(userMsg);
            
            // 保存用户消息到数据库
            saveConversationToDB(userId, sessionId, "user", message, "text", null);

            // 如果是新会话的第一条用户消息，自动生成会话标题
            if (history.size() == 2) { // 只有system message + 当前消息
                generateAndSetSessionTitle(userId, sessionId, message);
            }

            // 调用Spring AI获取回复
            String aiResponse = callAIService(history);

            // 检测AI回复中是否包含路线信息，如果有则自动生成地图路线
            String enhancedResponse = detectAndGenerateRoutes(aiResponse, message);

            // 添加AI回复到历史
            AssistantMessage assistantMsg = new AssistantMessage(enhancedResponse);
            history.add(assistantMsg);
            
            // 保存AI回复到数据库
            saveConversationToDB(userId, sessionId, "assistant", enhancedResponse, "text", null);

            // 限制历史长度
            if (history.size() > 20) {
                history.subList(1, 3).clear(); // 保留system prompt和最近的历史
            }

            log.info("AI回复 [{}]: {}", userId, enhancedResponse);
            return enhancedResponse;

        } catch (Exception e) {
            log.error("处理消息失败", e);
            return "抱歉，我遇到了一些问题，请稍后再试。错误信息：" + e.getMessage();
        }
    }

    /**
     * 调用Spring AI服务
     */
    private String callAIService(List<Message> messages) {
        try {
            log.info("调用Spring AI ChatClient: messages={}", messages.size());

            // 使用Spring AI的ChatClient进行对话
            ChatResponse response = chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();

            if (response == null || response.getResult() == null) {
                log.warn("Spring AI返回空响应");
                return "抱歉，我暂时无法回答这个问题。";
            }

            String content = response.getResult().getOutput().getText();
            
            if (content == null || content.isEmpty()) {
                log.warn("Spring AI返回空内容");
                return "抱歉，我暂时无法回答这个问题。";
            }
            
            log.debug("Spring AI响应: {}", content);
            return content;

        } catch (Exception e) {
            log.error("调用Spring AI失败", e);
            
            // 根据异常类型提供更具体的错误信息
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "❌ 认证失败（401）\n\n可能原因：\n" +
                       "1. API Key 错误或未配置\n" +
                       "2. API Key 格式不正确\n" +
                       "3. API Key 已过期或被禁用\n\n" +
                       "请检查 application-dev.yml 中的 spring.ai.openai.api-key 配置。";
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
     * 检测AI回复中的路线信息并自动生成地图路线
     * 支持驾车、步行、公交三种出行方式
     * @param aiResponse AI的原始回复
     * @param userMessage 用户的原始消息
     * @return 增强后的回复（包含地图路线数据）
     */
    private String detectAndGenerateRoutes(String aiResponse, String userMessage) {
        try {
            // 检测是否包含路线相关的关键词
            boolean hasRouteKeywords = aiResponse.contains("路线") || 
                                      aiResponse.contains("怎么走") || 
                                      aiResponse.contains("前往") ||
                                      aiResponse.contains("交通") ||
                                      aiResponse.contains("距离") ||
                                      aiResponse.contains("车程") ||
                                      aiResponse.contains("步行") ||
                                      aiResponse.contains("公交") ||
                                      aiResponse.contains("地铁") ||
                                      userMessage.contains("路线") ||
                                      userMessage.contains("怎么走") ||
                                      userMessage.contains("前往") ||
                                      userMessage.contains("怎么到") ||
                                      userMessage.matches(".*从.+到.+") ||
                                      userMessage.matches(".*去.+怎么.*");
            
            if (!hasRouteKeywords) {
                return aiResponse;
            }
            
            log.info("检测到路线相关对话，尝试提取地点信息");
            
            // 检测出行方式
            String travelMode = detectTravelMode(aiResponse, userMessage);
            log.info("检测到出行方式: {}", travelMode);
            
            // 尝试从AI回复和用户消息中提取起点和终点
            RouteInfo routeInfo = extractRouteFromText(aiResponse);
            if (routeInfo == null) {
                routeInfo = extractRouteFromText(userMessage);
            }
            
            if (routeInfo != null && routeInfo.getOrigin() != null && routeInfo.getDestination() != null) {
                log.info("提取到路线信息: {} -> {}，出行方式: {}", routeInfo.getOrigin(), routeInfo.getDestination(), travelMode);
                
                // 根据出行方式调用不同的路线规划服务
                Map<String, Object> routeData;
                switch (travelMode) {
                    case "walking":
                        routeData = mapRouteService.getWalkingRoute(routeInfo.getOrigin(), routeInfo.getDestination());
                        break;
                    case "transit":
                        String city = routeInfo.getCity() != null ? routeInfo.getCity() : "北京";
                        routeData = mapRouteService.getTransitRoute(routeInfo.getOrigin(), routeInfo.getDestination(), city);
                        break;
                    case "driving":
                    default:
                        routeData = mapRouteService.getDrivingRoute(routeInfo.getOrigin(), routeInfo.getDestination());
                        break;
                }
                
                // 如果成功获取路线，将路线数据附加到回复中
                if (!routeData.containsKey("error")) {
                    log.info("成功获取{}路线数据，附加到AI回复中", travelMode);
                    
                    // 构造包含路线数据的回复
                    StringBuilder enhancedResponse = new StringBuilder(aiResponse);
                    enhancedResponse.append("\n\n---\n\n");
                    
                    String modeIcon = getTravelModeIcon(travelMode);
                    String modeName = getTravelModeName(travelMode);
                    enhancedResponse.append("🗺️ **").append(modeName).append("路线详情**\n\n");
                    enhancedResponse.append("📍 距离：").append(routeData.get("distance")).append("\n");
                    enhancedResponse.append("⏱️ 预计时间：").append(routeData.get("duration")).append("\n\n");
                    
                    // 根据出行方式显示不同的路线步骤
                    if ("transit".equals(travelMode) && routeData.containsKey("segments")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> segments = (List<Map<String, String>>) routeData.get("segments");
                        if (segments != null && !segments.isEmpty()) {
                            enhancedResponse.append("🚌 **乘车方案：**\n\n");
                            for (int i = 0; i < Math.min(segments.size(), 8); i++) {
                                Map<String, String> seg = segments.get(i);
                                String segType = seg.getOrDefault("type", "");
                                String instruction = seg.getOrDefault("instruction", "");
                                String distance = seg.getOrDefault("distance", "");
                                String line = seg.getOrDefault("line", "");
                                enhancedResponse.append((i + 1)).append(". ");
                                if ("公交".equals(segType)) {
                                    enhancedResponse.append("🚌 ").append(instruction);
                                    if (!line.isEmpty()) enhancedResponse.append(" (").append(line).append(")");
                                } else if ("步行".equals(segType)) {
                                    enhancedResponse.append("🚶 ").append(instruction);
                                } else if ("地铁/火车".equals(segType)) {
                                    enhancedResponse.append("🚇 ").append(instruction);
                                    if (!line.isEmpty()) enhancedResponse.append(" (").append(line).append(")");
                                } else {
                                    enhancedResponse.append(instruction);
                                }
                                if (!distance.isEmpty()) enhancedResponse.append(" ").append(distance);
                                enhancedResponse.append("\n");
                            }
                            if (segments.size() > 8) {
                                enhancedResponse.append("... 共").append(segments.size()).append("个步骤\n");
                            }
                        }
                    } else if (routeData.containsKey("steps")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> steps = (List<Map<String, String>>) routeData.get("steps");
                        if (steps != null && !steps.isEmpty()) {
                            enhancedResponse.append(modeIcon).append(" **路线步骤：**\n\n");
                            for (int i = 0; i < Math.min(steps.size(), 5); i++) {
                                Map<String, String> step = steps.get(i);
                                enhancedResponse.append((i + 1)).append(". ")
                                    .append(step.get("instruction"))
                                    .append(" (").append(step.get("distance"))
                                    .append(", ").append(step.get("duration")).append(")\n");
                            }
                            if (steps.size() > 5) {
                                enhancedResponse.append("... 共").append(steps.size()).append("个步骤\n");
                            }
                        }
                    }
                    
                    // 附加路线数据供前端渲染地图
                    enhancedResponse.append("\n<!--ROUTE_DATA_START-->");
                    enhancedResponse.append(com.alibaba.fastjson.JSON.toJSONString(routeData));
                    enhancedResponse.append("<!--ROUTE_DATA_END-->");
                    
                    // 生成静态地图图片URL
                    String staticMapUrl = mapRouteService.generateStaticMapUrl(
                        routeInfo.getOrigin(),
                        routeInfo.getDestination(),
                        600,  // 宽度600px
                        400   // 高度400px
                    );
                    
                    if (staticMapUrl != null) {
                        enhancedResponse.append("\n\n![路线地图](");
                        enhancedResponse.append(staticMapUrl);
                        enhancedResponse.append(")");
                        log.info("已添加静态地图图片到回复中");
                    }
                    
                    return enhancedResponse.toString();
                } else {
                    log.warn("获取路线数据失败: {}", routeData.get("message"));
                }
            }
            
            return aiResponse;
            
        } catch (Exception e) {
            log.error("检测和生成路线失败", e);
            return aiResponse; // 失败时返回原始回复
        }
    }

    /**
     * 检测出行方式（驾车/步行/公交）
     */
    private String detectTravelMode(String aiResponse, String userMessage) {
        String combined = aiResponse + " " + userMessage;
        
        // 步行关键词
        if (combined.contains("步行") || combined.contains("走路") || combined.contains("徒步") ||
            combined.contains("走着去") || combined.contains("漫步")) {
            return "walking";
        }
        
        // 公交/地铁关键词
        if (combined.contains("公交") || combined.contains("地铁") || combined.contains("乘车") ||
            combined.contains("坐车") || combined.contains("搭车") || combined.contains("换乘") ||
            combined.contains("轨道交通")) {
            return "transit";
        }
        
        // 默认驾车
        return "driving";
    }

    /**
     * 获取出行方式图标
     */
    private String getTravelModeIcon(String mode) {
        switch (mode) {
            case "walking": return "🚶";
            case "transit": return "🚌";
            case "driving": 
            default: return "🚗";
        }
    }

    /**
     * 获取出行方式名称
     */
    private String getTravelModeName(String mode) {
        switch (mode) {
            case "walking": return "步行";
            case "transit": return "公交";
            case "driving": 
            default: return "驾车";
        }
    }

    /**
     * 从文本中提取路线信息（起点和终点）
     * 支持多种中文表达模式
     * @param text 要分析的文本
     * @return 路线信息，如果无法提取则返回null
     */
    private RouteInfo extractRouteFromText(String text) {
        RouteInfo routeInfo = new RouteInfo();
        
        // 扩大字符匹配范围，支持空格和更多中文字符
        String placePattern = "([\\u4e00-\\u9fa5_a-zA-Z0-9\\s·]+?)";
        
        // 模式1: "从A到B" / "从A去B"
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("从" + placePattern + "[到去]" + placePattern);
        java.util.regex.Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            routeInfo.setOrigin(matcher1.group(1).trim());
            routeInfo.setDestination(matcher1.group(2).trim());
            return routeInfo;
        }
        
        // 模式2: "A到B的路线" / "A到B怎么走"
        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile(placePattern + "到" + placePattern + "(?:的路线|怎么走|多远|多久)");
        java.util.regex.Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            routeInfo.setOrigin(matcher2.group(1).trim());
            routeInfo.setDestination(matcher2.group(2).trim());
            return routeInfo;
        }
        
        // 模式3: "A前往B" / "A去往B"
        java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile(placePattern + "(?:前往|去往)" + placePattern);
        java.util.regex.Matcher matcher3 = pattern3.matcher(text);
        if (matcher3.find()) {
            routeInfo.setOrigin(matcher3.group(1).trim());
            routeInfo.setDestination(matcher3.group(2).trim());
            return routeInfo;
        }
        
        // 模式4: "A-B" / "A—B" 短横线分隔
        java.util.regex.Pattern pattern4 = java.util.regex.Pattern.compile(placePattern + "[—\\-]" + placePattern);
        java.util.regex.Matcher matcher4 = pattern4.matcher(text);
        if (matcher4.find()) {
            routeInfo.setOrigin(matcher4.group(1).trim());
            routeInfo.setDestination(matcher4.group(2).trim());
            return routeInfo;
        }
        
        // 模式5: "去B" (起点默认为"我的位置")
        java.util.regex.Pattern pattern5 = java.util.regex.Pattern.compile("(?:去|到达|抵达)" + placePattern);
        java.util.regex.Matcher matcher5 = pattern5.matcher(text);
        if (matcher5.find()) {
            routeInfo.setOrigin("我的位置");
            routeInfo.setDestination(matcher5.group(1).trim());
            return routeInfo;
        }
        
        return null;
    }

    /**
     * 路线信息内部类
     */
    private static class RouteInfo {
        private String origin;
        private String destination;
        private String city; // 城市（用于公交路线查询）

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
     * 清空对话历史
     */
    public void clearHistory(String userId) {
        // 如果 userId 为空，尝试从登录上下文获取
        if (userId == null || userId.isEmpty()) {
            String currentId = BaseContext.getCurrentId();
            if (currentId != null && !currentId.isEmpty()) {
                userId = currentId;
            }
        }
        
        // 清空当前用户的会话
        String sessionId = userCurrentSession.get(userId);
        if (sessionId != null) {
            conversationHistory.remove(sessionId);
        }
        userCurrentSession.remove(userId);
        
        // 删除数据库中的对话记录
        if (userId != null && !userId.equals("anonymous_user")) {
            conversationMapper.deleteByUserId(userId);
            log.info("已清空用户 [{}] 的对话历史", userId);
        }
    }

    /**
     * 自动生成并设置会话标题
     * 根据用户的第一条消息内容生成简洁的标题
     */
    private void generateAndSetSessionTitle(String userId, String sessionId, String firstMessage) {
        try {
            String title = extractTitleFromMessage(firstMessage);
            
            // 更新数据库中的会话标题
            conversationMapper.updateSessionTitle(userId, sessionId, title);
            
            log.info("已为会话 [{}] 自动生成标题: {}", sessionId, title);
        } catch (Exception e) {
            log.error("生成会话标题失败", e);
            // 失败时使用默认标题
            conversationMapper.updateSessionTitle(userId, sessionId, "新对话");
        }
    }

    /**
     * 从消息内容中提取会话标题
     * 规则：
     * 1. 如果是问题，提取关键词
     * 2. 如果是任务，提取任务类型
     * 3. 默认使用前15个字符
     */
    private String extractTitleFromMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "新对话";
        }

        // 去除前后空白
        String trimmed = message.trim();
        
        // 如果消息很短，直接使用
        if (trimmed.length() <= 15) {
            return trimmed;
        }

        // 提取标题的逻辑
        String title = trimmed;
        
        // 1. 如果是提问，尝试提取主题
        if (trimmed.startsWith("什么") || trimmed.startsWith("如何") || trimmed.startsWith("怎么") ||
            trimmed.startsWith("为什么") || trimmed.startsWith("能否") || trimmed.startsWith("请")) {
            // 取前20个字符作为标题
            title = trimmed.substring(0, Math.min(20, trimmed.length()));
        }
        // 2. 如果是任务相关
        else if (trimmed.contains("任务") || trimmed.contains("待办") || trimmed.contains("todo")) {
            title = "任务管理";
        }
        // 3. 如果是文件相关
        else if (trimmed.contains("文件") || trimmed.contains("创建") || trimmed.contains("保存")) {
            title = "文件操作";
        }
        // 4. 如果是天气查询
        else if (trimmed.contains("天气")) {
            title = "天气查询";
        }
        // 5. 如果是计算
        else if (trimmed.contains("计算") || trimmed.contains("等于")) {
            title = "数学计算";
        }
        // 6. 默认：取前15个字符
        else {
            title = trimmed.substring(0, Math.min(15, trimmed.length()));
        }

        // 如果标题太长，截断并添加省略号
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }

        return title;
    }

    /**
     * 创建新会话
     */
    public String createNewSession(String userId, String sessionTitle) {
        // 如果 userId 为空，尝试从登录上下文获取
        if (userId == null || userId.isEmpty()) {
            String currentId = BaseContext.getCurrentId();
            if (currentId != null && !currentId.isEmpty()) {
                userId = currentId;
            } else {
                userId = "anonymous_user";
            }
        }
        
        String newSessionId = "session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        userCurrentSession.put(userId, newSessionId);
        
        // 初始化对话历史
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage(SYSTEM_PROMPT));
        conversationHistory.put(newSessionId, history);
        
        log.info("为用户 [{}] 创建新会话: {}, 标题: {}", userId, newSessionId, sessionTitle);
        return newSessionId;
    }

    /**
     * 切换到指定会话
     */
    public void switchSession(String userId, String sessionId) {
        // 如果 userId 为空，尝试从登录上下文获取
        if (userId == null || userId.isEmpty()) {
            String currentId = BaseContext.getCurrentId();
            if (currentId != null && !currentId.isEmpty()) {
                userId = currentId;
            }
        }
        
        userCurrentSession.put(userId, sessionId);
        
        // 如果会话历史不存在，初始化它
        if (!conversationHistory.containsKey(sessionId)) {
            List<Message> history = new ArrayList<>();
            history.add(new SystemMessage(SYSTEM_PROMPT));
            conversationHistory.put(sessionId, history);
        }
        
        log.info("用户 [{}] 切换到会话: {}", userId, sessionId);
    }

    /**
     * 获取用户当前会话ID
     */
    private String getCurrentSessionId(String userId) {
        String sessionId = userCurrentSession.get(userId);
        if (sessionId == null) {
            // 如果没有当前会话，创建一个新会话
            sessionId = createNewSession(userId, "新对话");
        }
        return sessionId;
    }

    /**
     * 获取对话历史（从数据库加载）
     */
    public List<Map<String, String>> getHistory(String userId, String sessionId) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                } else {
                    log.warn("无法获取用户ID");
                    return new ArrayList<>();
                }
            }
            
            // 如果 sessionId 为空，使用当前会话
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = getCurrentSessionId(userId);
                log.info("未提供sessionId，使用当前会话: {}", sessionId);
            }
            
            log.info("开始获取会话历史 - userId: {}, sessionId: {}", userId, sessionId);
            
            // 从数据库加载历史对话
            List<Conversation> conversations = conversationMapper.findBySessionId(userId, sessionId, 100);
            
            if (conversations == null) {
                log.warn("数据库查询返回null - userId: {}, sessionId: {}", userId, sessionId);
                conversations = new ArrayList<>();
            }
            
            log.info("从数据库查询到 {} 条历史记录", conversations.size());
            
            List<Map<String, String>> result = new ArrayList<>();
            
            for (Conversation conv : conversations) {
                if (conv != null) {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", conv.getRole() != null ? conv.getRole() : "unknown");
                    map.put("content", conv.getContent() != null ? conv.getContent() : "");
                    map.put("id", conv.getId() != null ? conv.getId().toString() : "");
                    map.put("createdAt", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : "");
                    result.add(map);
                }
            }
            
            log.info("成功获取会话历史 [{}] 共 {} 条记录", sessionId, result.size());
            return result;
        } catch (Exception e) {
            log.error("获取对话历史失败 - userId: {}, sessionId: {}", userId, sessionId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取最近的对话记录（从数据库）
     */
    public List<com.clover.controller.AgentController.ConversationSummary> getRecentConversations(String userId, int limit) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
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
     * 获取用户的会话列表
     */
    public List<ConversationMapper.SessionInfo> getSessionList(String userId) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                } else {
                    userId = "anonymous_user";
                }
            }
            
            return conversationMapper.findSessionsByUserId(userId);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 删除会话
     */
    public void deleteSession(String userId, String sessionId) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                }
            }
            
            // 从内存中移除
            conversationHistory.remove(sessionId);
            
            // 如果是当前会话，清除当前会话映射
            String currentSessionId = userCurrentSession.get(userId);
            if (sessionId.equals(currentSessionId)) {
                userCurrentSession.remove(userId);
            }
            
            // 从数据库中删除
            conversationMapper.deleteBySessionId(userId, sessionId);
            log.info("已删除用户 [{}] 的会话: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("删除会话失败", e);
        }
    }
    
    /**
     * 更新会话标题
     */
    public void updateSessionTitle(String userId, String sessionId, String sessionTitle) {
        try {
            // 如果 userId 为空，尝试从登录上下文获取
            if (userId == null || userId.isEmpty()) {
                String currentId = BaseContext.getCurrentId();
                if (currentId != null && !currentId.isEmpty()) {
                    userId = currentId;
                }
            }
            
            conversationMapper.updateSessionTitle(userId, sessionId, sessionTitle);
            log.info("已更新用户 [{}] 的会话 [{}] 标题为: {}", userId, sessionId, sessionTitle);
        } catch (Exception e) {
            log.error("更新会话标题失败", e);
        }
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
            
            // 获取并设置会话标题（确保同一session_id的所有记录都有相同的title）
            String sessionTitle = getSessionTitleFromDB(userId, sessionId);
            conversation.setSessionTitle(sessionTitle);
            
            conversationMapper.insert(conversation);
            log.debug("对话已保存到数据库: userId={}, sessionId={}, title={}", userId, sessionId, sessionTitle);
        } catch (Exception e) {
            log.error("保存对话到数据库失败", e);
            // 不抛出异常，避免影响正常聊天流程
        }
    }
    
    /**
     * 从数据库获取会话标题
     */
    private String getSessionTitleFromDB(String userId, String sessionId) {
        try {
            List<ConversationMapper.SessionInfo> sessions = conversationMapper.findSessionsByUserId(userId);
            for (ConversationMapper.SessionInfo info : sessions) {
                if (info.getSessionId().equals(sessionId)) {
                    return info.getSessionTitle() != null ? info.getSessionTitle() : "新对话";
                }
            }
        } catch (Exception e) {
            log.warn("获取会话标题失败，使用默认标题", e);
        }
        return "新对话";
    }
}
