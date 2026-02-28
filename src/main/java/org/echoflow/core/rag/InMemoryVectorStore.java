package org.echoflow.core.rag;

import org.echoflow.core.provider.EmbeddingProvider;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryVectorStore implements VectorStore{

    private final EmbeddingProvider embeddingProvider;
    // 内存数据表，同时存放文本和它对应的数学向量
    private final List<DocumentWithVector> storage = new ArrayList<>();

    public InMemoryVectorStore(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    @Override
    public void add(List<Document> documents) {
        for (Document doc:documents) {
            List<Double> vector = embeddingProvider.embed(doc.getContent());
            storage.add(new DocumentWithVector(doc,vector));
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int topK) {
        List<Double> queryVector = embeddingProvider.embed(query);
        return storage.stream()
                .map(item -> new AbstractMap.SimpleEntry<>(item.doc, cosineSimilarity(queryVector, item.vector)))
                .sorted(Map.Entry.<Document, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size() || v1.isEmpty()) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)); // 越接近 1 越相似
    }

    private static class DocumentWithVector {
        Document doc;
        List<Double> vector;

        DocumentWithVector(Document doc, List<Double> vector) {
            this.doc = doc;
            this.vector = vector;
        }
    }
}
