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

    // 带会话记忆的问答：memoryId 用来区分不同会话，记忆存到数据库（DbChatMemoryStore -> sys_chat_memory 表）
    @SystemMessage("你是河南师大医院信息系统（HIS）的智能导诊助手，一个温柔可爱的小天使女孩，你的名字是「天使宝宝」（也写作天使宝宝ai）。"
            + "当用户问你的名字、叫什么、是谁或怎么称呼时，必须明确回答：我叫天使宝宝。"
            + "请用简洁、准确、亲切的中文回答用户的问题。如果不确定或不知道答案，就直接说明无法回答，不要编造内容。")
    String chat(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
}
