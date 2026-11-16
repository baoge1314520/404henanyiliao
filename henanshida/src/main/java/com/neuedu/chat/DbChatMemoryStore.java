package com.neuedu.chat;

import com.neuedu.entity.SysChatMemory;
import com.neuedu.mapper.SysChatMemoryMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库版会话记忆存储：实现 langchain4j 的 {@link ChatMemoryStore}，
 * 把每个会话的历史消息持久化到 PostgreSQL 的 sys_chat_memory 表。
 *
 * <p>与课上演示的 {@link RedisChatMemoryStore} 三个方法一一对应，
 * 只是把 Redis 的 KV 存储换成数据库表（一个 memoryId 对应一行）：</p>
 * <ul>
 *   <li>getMessages    —— 按主键 memoryId 查一行，把 messages JSON 反序列化为消息列表；</li>
 *   <li>updateMessages —— 把整段历史序列化为 JSON，按主键 upsert（存在则更新，不存在则插入）；</li>
 *   <li>deleteMessages —— 按主键删除该行。</li>
 * </ul>
 *
 * <p>消息的序列化/反序列化复用 langchain4j 自带的
 * {@link ChatMessageSerializer} / {@link ChatMessageDeserializer}，与 Redis 版完全一致，
 * 因此切换存储介质不影响记忆内容。加 {@code @Primary} 后，Spring 按类型注入
 * {@code ChatMemoryStore} 时优先选用本实现，从而把记忆从 Redis 切换到数据库；
 * 老师的 RedisChatMemoryStore 仍保留作对照。</p>
 */
@Component
@Primary
public class DbChatMemoryStore implements ChatMemoryStore {

    @Resource
    private SysChatMemoryMapper chatMemoryMapper;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        SysChatMemory row = chatMemoryMapper.selectOneById(memoryId.toString());
        if (row == null || row.getMessages() == null || row.getMessages().isBlank()) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(row.getMessages());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();
        String json = ChatMessageSerializer.messagesToJson(messages);
        SysChatMemory row = SysChatMemory.builder()
                .memoryId(id)
                .messages(json)
                .updateTime(LocalDateTime.now())
                .build();
        // 一个会话一行：已存在则按主键更新，不存在则插入
        if (chatMemoryMapper.selectOneById(id) != null) {
            chatMemoryMapper.update(row);
        } else {
            chatMemoryMapper.insert(row);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        chatMemoryMapper.deleteById(memoryId.toString());
    }
}