package com.neuedu.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisChatMemoryStore implements ChatMemoryStore {
    @Resource(name = "wyRedisTemplate")
    RedisTemplate<String, Object> redisTemplate;
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        if (redisTemplate.hasKey(id)) {
            String messagesJson = redisTemplate.opsForValue().get(id).toString();
            List<ChatMessage> chatMessages = ChatMessageDeserializer.messagesFromJson(messagesJson);
            return chatMessages;
        }
        return List.of();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();
        String message = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(id, message);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = memoryId.toString();
        if (redisTemplate.hasKey(id)) {
            redisTemplate.delete(id);
        }
    }
}
