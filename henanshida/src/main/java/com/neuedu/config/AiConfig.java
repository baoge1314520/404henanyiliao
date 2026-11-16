package com.neuedu.config;

import com.neuedu.rag.LexicalContentRetriever;
import com.neuedu.service.Assistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 问答装配。
 * ChatModel 由 langchain4j-open-ai-spring-boot-starter 根据 application.yml 自动创建；
 * ChatMemoryStore 默认用数据库版 DbChatMemoryStore（@Primary），把每个会话的历史消息存到
 * PostgreSQL 的 sys_chat_memory 表；老师的 RedisChatMemoryStore 仍保留作对照。
 *
 * <p>RAG：装配本地词法检索器 {@link LexicalContentRetriever}（BM25，零网络），
 * 通过 RetrievalAugmentor 在每次提问时自动检索医院知识库并把参考资料注入上下文；
 * storeRetrievedContentInChatMemory(false) 保证检索内容不写进会话记忆，记忆只保留原始问答。</p>
 */
@Configuration
public class AiConfig {

    /** 检索增强提示模板：要求模型优先依据参考资料作答，不足时如实说明，不编造。 */
    private static final String RAG_PROMPT = """
            请优先依据下面【参考资料】中的信息回答用户的问题，做到准确、简洁、亲切。
            若参考资料与问题相关，请以其为准；若资料不足以回答，请如实说明无法确定，不要编造。
            直接用自然语言作答，不要原样罗列参考资料，也不要出现「参考资料」等字样。

            【参考资料】
            {{contents}}

            【用户问题】
            {{userMessage}}""";

    @Bean
    Assistant assistant(ChatModel chatModel, ChatMemoryStore chatMemoryStore, LexicalContentRetriever ragRetriever) {
        ContentInjector injector = DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from(RAG_PROMPT))
                .build();
        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(ragRetriever)
                .contentInjector(injector)
                .build();
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(chatMemoryStore)
                        .build())
                .retrievalAugmentor(augmentor)
                .storeRetrievedContentInChatMemory(false)
                .build();
    }
}