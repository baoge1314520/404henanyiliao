package com.neuedu.controller;

import com.neuedu.rag.LexicalContentRetriever;
import com.neuedu.rag.RagKnowledgeBase;
import com.neuedu.rag.RagSource;
import com.neuedu.service.Assistant;
import com.neuedu.vo.Result;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 问答 控制层。
 * 复用老师的 Assistant + 数据库版 DbChatMemoryStore，按 memoryId 区分会话、保留上下文。
 * 已接入 RAG：每次提问先检索医院知识库（演示数据），由 AiConfig 的检索增强器自动注入上下文，
 * 同时把命中的「参考来源」一并返回给前端展示，让答案可溯源。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private Assistant assistant;
    @Resource
    private ChatMemoryStore chatMemoryStore;
    @Resource
    private LexicalContentRetriever ragRetriever;
    @Resource
    private RagKnowledgeBase ragKnowledgeBase;

    @PostMapping("chat")
    Result chat(String memoryId, String message) {
        if (message == null || message.isBlank()) {
            return Result.fail("请输入要提问的内容");
        }
        if (memoryId == null || memoryId.isBlank()) {
            memoryId = "default";
        }
        try {
            // 先取参考来源用于前端展示；真正的上下文注入由 AiServices 的检索增强器在 chat 内部完成
            List<RagSource> sources = ragRetriever.searchSources(message, 3);
            String answer = assistant.chat(memoryId, message);
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("answer", answer);
            content.put("sources", sources);
            content.put("ragUsed", !sources.isEmpty());
            return Result.success(content);
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

    /** 知识库 / 检索索引状态。 */
    @GetMapping("rag/status")
    Result ragStatus() {
        return Result.success(ragRetriever.status());
    }

    /** 热重建知识库与索引（编辑 rag/knowledge.json 后无需重启）。 */
    @PostMapping("rag/reindex")
    Result ragReindex() {
        ragKnowledgeBase.reload();
        ragRetriever.reindex();
        return Result.success(ragRetriever.status(), "知识库已重建");
    }
}