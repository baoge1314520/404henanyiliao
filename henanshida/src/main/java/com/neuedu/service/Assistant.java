package com.neuedu.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;

import java.util.List;

public interface Assistant {
    String chat(String userMessage);
    String chat(UserMessage userMessage);
    String chat(List<ChatMessage> messages);

    // 带会话记忆的问答：memoryId 用来区分不同会话，记忆存到 Redis（RedisChatMemoryStore）
    @SystemMessage("你是河南师大医院信息系统（HIS）的 AI 助手，请用简洁、准确的中文回答用户的问题。"
            + "如果不确定或不知道答案，就直接说明无法回答，不要编造内容。")
    String chat(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
}
