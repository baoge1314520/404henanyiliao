package com.neuedu.rag;

import java.util.List;

/**
 * 知识库文档（一条可检索的知识片段）。
 * 由 classpath:rag/knowledge.json 反序列化而来，字段公开以便 Jackson 直接读写。
 */
public class RagDocument {
    /** 文档唯一标识 */
    public String id;
    /** 标题 */
    public String title;
    /** 分类，如「科室介绍」「挂号服务」 */
    public String category;
    /** 正文内容 */
    public String content;
    /** 来源说明（演示数据统一标注） */
    public String source;
    /** 关键词标签，用于检索加权 */
    public List<String> tags;

    public RagDocument() {
    }
}