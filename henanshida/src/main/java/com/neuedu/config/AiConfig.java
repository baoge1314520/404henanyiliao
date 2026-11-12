package com.neuedu.config;

import com.neuedu.service.Assistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 问答装配。
 * ChatModel 由 langchain4j-open-ai-spring-boot-starter 根据 application.yml 自动创建；
 * ChatMemoryStore 用的是老师写的 RedisChatMemoryStore，把每个会话的历史消息存到 Redis。
 */
@Configuration
public class AiConfig {

    @Bean
    Assistant assistant(ChatModel chatModel, ChatMemoryStore chatMemoryStore) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .build();
    }
}