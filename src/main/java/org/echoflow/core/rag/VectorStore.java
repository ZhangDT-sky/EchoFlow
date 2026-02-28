package org.echoflow.core.rag;

import java.util.List;

public interface VectorStore {
    void add(List<Document> documents);
    List<Document> similaritySearch(String query, int topK);
}
