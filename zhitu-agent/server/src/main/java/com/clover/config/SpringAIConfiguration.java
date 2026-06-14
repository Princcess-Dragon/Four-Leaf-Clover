package com.clover.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI配置类
 */
@Configuration
public class SpringAIConfiguration {

    /**
     * 创建ChatClient Bean
     * Spring AI会自动注入OpenAiChatModel
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
