package com.neuedu.controller;

import com.neuedu.service.Assistant;
import com.neuedu.vo.Result;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 问答 控制层。
 * 复用老师的 Assistant + RedisChatMemoryStore，按 memoryId 区分会话、保留上下文。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private Assistant assistant;
    @Resource
    private ChatMemoryStore chatMemoryStore;

    @PostMapping("chat")
    Result chat(String memoryId, String message) {
        if (message == null || message.isBlank()) {
            return Result.fail("请输入要提问的内容");
        }
        if (memoryId == null || memoryId.isBlank()) {
            memoryId = "default";
        }
        try {
            String answer = assistant.chat(memoryId, message);
            return Result.success(answer);
        } catch (Exception e) {
            // 上游模型报错（如 api-key 失效、网络超时）时，返回友好提示而不是抛 500 堆栈
            return Result.fail("AI 服务调用失败：" + e.getMessage());
        }
    }

    @PostMapping("clear")
    Result clear(String memoryId) {
        if (memoryId == null || memoryId.isBlank()) {
            memoryId = "default";
        }
        chatMemoryStore.deleteMessages(memoryId);
        return Result.success(null, "已清空对话记忆");
    }
}