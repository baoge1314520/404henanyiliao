package com.neuedu.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 词法 RAG 检索器（BM25 + 中文一元/二元分词）。
 *
 * <p>不依赖任何外部向量服务或额外模型，纯本地、零网络、零额外依赖，保证离线可用。
 * 对中文按「单字 + 相邻二字」切词，对英文/数字按小写单词切词，用 BM25 计算查询与知识片段的相关度，
 * 取相关度最高的若干条注入大模型上下文，实现检索增强生成（RAG）。</p>
 *
 * <p>实现 langchain4j 的 {@link ContentRetriever}，由 AiConfig 装配进 AiServices；
 * 同时对外暴露 {@link #searchSources} 供控制器把「参考来源」返回给前端展示。</p>
 */
@Component
public class LexicalContentRetriever implements ContentRetriever {

    private static final Logger log = LoggerFactory.getLogger(LexicalContentRetriever.class);

    /** BM25 参数 */
    private static final double K1 = 1.5;
    private static final double B = 0.75;
    /** 命中标题/标签时的加权 */
    private static final double TITLE_BOOST = 1.6;
    /** 相关度绝对下限与相对下限（相对最高分），过滤噪声 */
    private static final double MIN_SCORE = 0.6;
    private static final double REL_FLOOR = 0.4;
    /** 默认注入条数 */
    private static final int TOP_K = 3;

    @Resource
    private RagKnowledgeBase knowledgeBase;

    private volatile Index index;

    @PostConstruct
    public void init() {
        reindex();
    }

    /** 依据当前知识库重建倒排索引。 */
    public synchronized void reindex() {
        this.index = build(knowledgeBase.documents());
        log.info("[RAG] 检索索引重建完成：{} 条文档，{} 个词项", index.n, index.df.size());
    }

    /* ---------------- langchain4j ContentRetriever ---------------- */

    @Override
    public List<Content> retrieve(Query query) {
        String text = query == null ? null : query.text();
        List<RagHit> hits = search(text, TOP_K);
        log.info("[RAG] retrieve query='{}' -> {} hits", text, hits.size());
        List<Content> contents = new ArrayList<>(hits.size());
        for (RagHit h : hits) {
            contents.add(Content.from(format(h.doc)));
        }
        return contents;
    }

    /* ---------------- 对外检索 ---------------- */

    /** 检索并返回前端可展示的参考来源。 */
    public List<RagSource> searchSources(String query, int topK) {
        List<RagHit> hits = search(query, topK);
        List<RagSource> out = new ArrayList<>(hits.size());
        for (RagHit h : hits) {
            RagDocument d = h.doc;
            String snippet = d.content == null ? "" : d.content.replaceAll("\\s+", " ").trim();
            if (snippet.length() > 110) {
                snippet = snippet.substring(0, 110) + "…";
            }
            out.add(new RagSource(d.id, d.title, d.category, snippet,
                    Math.round(h.score * 100.0) / 100.0, d.source));
        }
        return out;
    }

    /** 知识库 / 索引状态。 */
    public Map<String, Object> status() {
        Index idx = this.index;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("engine", "BM25 词法检索（中文一元/二元分词，本地零网络）");
        m.put("docCount", idx == null ? 0 : idx.n);
        m.put("termCount", idx == null ? 0 : idx.df.size());
        m.put("topK", TOP_K);
        m.put("lastBuildTime", idx == null ? 0 : idx.builtAt);
        m.put("knowledgeLoadedAt", knowledgeBase.loadedAt());
        m.put("loadError", knowledgeBase.loadError());
        return m;
    }

    /* ---------------- BM25 核心 ---------------- */

    private List<RagHit> search(String query, int topK) {
        Index idx = this.index;
        if (idx == null || idx.n == 0 || query == null || query.isBlank()) {
            return List.of();
        }
        List<String> q = tokenize(query);
        if (q.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> qtf = new HashMap<>();
        for (String t : q) {
            qtf.merge(t, 1, Integer::sum);
        }
        double[] scores = new double[idx.n];
        double max = 0;
        for (Map.Entry<String, Integer> e : qtf.entrySet()) {
            String term = e.getKey();
            Integer df = idx.df.get(term);
            if (df == null) {
                continue;
            }
            double idf = Math.log(1 + (idx.n - df + 0.5) / (df + 0.5));
            for (int i = 0; i < idx.n; i++) {
                Integer tfObj = idx.tfs.get(i).get(term);
                if (tfObj == null) {
                    continue;
                }
                int tf = tfObj;
                double norm = tf + K1 * (1 - B + B * idx.docLen[i] / idx.avgdl);
                double s = idf * (tf * (K1 + 1)) / norm;
                if (idx.titleTerms.get(i).contains(term)) {
                    s *= TITLE_BOOST;
                }
                scores[i] += s;
                if (scores[i] > max) {
                    max = scores[i];
                }
            }
        }
        if (max <= 0) {
            return List.of();
        }
        double floor = Math.max(MIN_SCORE, max * REL_FLOOR);
        List<RagHit> hits = new ArrayList<>();
        for (int i = 0; i < idx.n; i++) {
            if (scores[i] >= floor) {
                hits.add(new RagHit(idx.docs.get(i), scores[i]));
            }
        }
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        if (hits.size() > topK) {
            hits = new ArrayList<>(hits.subList(0, topK));
        }
        return hits;
    }

    private Index build(List<RagDocument> docs) {
        int n = docs.size();
        List<Map<String, Integer>> tfs = new ArrayList<>(n);
        List<Set<String>> titleTerms = new ArrayList<>(n);
        Map<String, Integer> df = new HashMap<>();
        int[] docLen = new int[n];
        long total = 0;
        for (int i = 0; i < n; i++) {
            RagDocument d = docs.get(i);
            String tags = d.tags == null ? "" : String.join(" ", d.tags);
            String title = d.title == null ? "" : d.title;
            String content = d.content == null ? "" : d.content;
            List<String> toks = tokenize(title + " " + tags + " " + content);
            Map<String, Integer> tf = new HashMap<>();
            for (String t : toks) {
                tf.merge(t, 1, Integer::sum);
            }
            tfs.add(tf);
            docLen[i] = toks.size();
            total += toks.size();
            for (String t : tf.keySet()) {
                df.merge(t, 1, Integer::sum);
            }
            titleTerms.add(new HashSet<>(tokenize(title + " " + tags)));
        }
        double avgdl = n > 0 ? (double) total / n : 0;
        return new Index(docs, tfs, titleTerms, df, docLen, avgdl, n, System.currentTimeMillis());
    }

    /** 中文：单字 + 相邻二字；英文/数字：小写单词；其余视为分隔。 */
    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        char[] cs = text.toLowerCase().toCharArray();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            boolean han = c >= 0x4E00 && c <= 0x9FFF;
            boolean ascii = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (ascii) {
                word.append(c);
            } else {
                if (word.length() > 0) {
                    out.add(word.toString());
                    word.setLength(0);
                }
                if (han) {
                    out.add(String.valueOf(c));
                    if (i + 1 < cs.length) {
                        char nx = cs[i + 1];
                        if (nx >= 0x4E00 && nx <= 0x9FFF) {
                            out.add("" + c + nx);
                        }
                    }
                }
            }
        }
        if (word.length() > 0) {
            out.add(word.toString());
        }
        return out;
    }

    private static String format(RagDocument d) {
        return "《" + d.title + "》（分类：" + d.category + "）\n" + d.content + "\n—— 来源：" + d.source;
    }

    /* ---------------- 内部结构 ---------------- */

    public static final class RagHit {
        public final RagDocument doc;
        public final double score;

        RagHit(RagDocument doc, double score) {
            this.doc = doc;
            this.score = score;
        }
    }

    private static final class Index {
        final List<RagDocument> docs;
        final List<Map<String, Integer>> tfs;
        final List<Set<String>> titleTerms;
        final Map<String, Integer> df;
        final int[] docLen;
        final double avgdl;
        final int n;
        final long builtAt;

        Index(List<RagDocument> docs, List<Map<String, Integer>> tfs, List<Set<String>> titleTerms,
              Map<String, Integer> df, int[] docLen, double avgdl, int n, long builtAt) {
            this.docs = docs;
            this.tfs = tfs;
            this.titleTerms = titleTerms;
            this.df = df;
            this.docLen = docLen;
            this.avgdl = avgdl;
            this.n = n;
            this.builtAt = builtAt;
        }
    }
}