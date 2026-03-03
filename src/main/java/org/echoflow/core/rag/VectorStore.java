package org.echoflow.core.rag;

import java.util.List;

public interface VectorStore {
    /** 批量写入，建立 Embedding 索引 */
    void add(List<Document> documents);
    /** 纯向量语义相似度检索 */
    List<Document> similaritySearch(String query, int topK);

    /**
     * 混合检索（语义 + 关键词），子类可按需覆盖实现。
     * 默认退化为纯向量检索，保证向下兼容。
     */
    default List<Document> hybridSearch(String query, int topK){
        return similaritySearch(query,topK);
    }

}
