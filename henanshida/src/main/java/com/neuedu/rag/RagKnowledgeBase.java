package com.neuedu.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 医院知识库（演示数据）。
 * 启动时从 classpath:rag/knowledge.json 加载知识片段，供 RAG 检索器建立索引。
 * 支持热重建：调用 reload() 重新读取文件，无需重启服务。
 */
@Component
public class RagKnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeBase.class);
    private static final String LOCATION = "rag/knowledge.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<List<RagDocument>> docs = new AtomicReference<>(List.of());
    private volatile long loadedAt;
    private volatile String loadError;

    @PostConstruct
    public void init() {
        reload();
    }

    /** 重新读取知识库文件并做基础清洗。失败时保留上一份可用数据。 */
    public synchronized void reload() {
        try (InputStream in = new ClassPathResource(LOCATION).getInputStream()) {
            List<RagDocument> list = mapper.readValue(in, new TypeReference<List<RagDocument>>() {
            });
            List<RagDocument> clean = new ArrayList<>();
            for (RagDocument d : list) {
                if (d == null || d.content == null || d.content.isBlank()) {
                    continue;
                }
                if (d.id == null || d.id.isBlank()) {
                    d.id = "doc-" + (clean.size() + 1);
                }
                if (d.title == null || d.title.isBlank()) {
                    d.title = d.id;
                }
                if (d.category == null || d.category.isBlank()) {
                    d.category = "通用";
                }
                if (d.source == null || d.source.isBlank()) {
                    d.source = "河南师大医院知识库（演示数据）";
                }
                if (d.tags == null) {
                    d.tags = List.of();
                }
                clean.add(d);
            }
            docs.set(Collections.unmodifiableList(clean));
            loadedAt = System.currentTimeMillis();
            loadError = null;
            log.info("[RAG] 知识库加载完成，共 {} 条（演示数据）", clean.size());
        } catch (Exception e) {
            loadError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("[RAG] 知识库加载失败：{}（保留已有 {} 条）", loadError, docs.get().size());
        }
    }

    public List<RagDocument> documents() {
        return docs.get();
    }

    public int size() {
        return docs.get().size();
    }

    public long loadedAt() {
        return loadedAt;
    }

    public String loadError() {
        return loadError;
    }
}