package com.neuedu.rag;

/**
 * 返回给前端的「参考来源」。天使宝宝回答时附带，便于用户核对答案出处。
 */
public class RagSource {
    public String id;
    public String title;
    public String category;
    /** 内容摘要（截断） */
    public String snippet;
    /** 相关度得分（BM25） */
    public double score;
    public String source;

    public RagSource() {
    }

    public RagSource(String id, String title, String category, String snippet, double score, String source) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.snippet = snippet;
        this.score = score;
        this.source = source;
    }
}