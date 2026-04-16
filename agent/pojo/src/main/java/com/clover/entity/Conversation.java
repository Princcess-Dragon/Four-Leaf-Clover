package com.clover.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话历史实体类
 */
@Data
public class Conversation {
    /**
     * 对话ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色: user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型: text, image, file
     */
    private String messageType;

    /**
     * 工具调用信息(JSON)
     */
    private String toolCalls;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
