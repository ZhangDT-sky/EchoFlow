package org.echoflow.core.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoflow.core.provider.EmbeddingProvider;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis + RediSearch 的生产级向量数据库实现。
 * 利用 Redis 的 FT.SEARCH KNN 命令做向量余弦检索，性能远超内存遍历。
 *
 * 前置条件：Redis 服务端需要加载 RediSearch 模块（Docker 用 redis/redis-stack 镜像）。
 */
public class RedisVectorStore implements VectorStore{

    private static final String KEY_PREFIX = "echoflow:rag:doc:";
    private static final String INDEX_NAME = "echoflow_rag_index";

    private final StringRedisTemplate redisTemplate;
    private final EmbeddingProvider embeddingProvider;
    private final ObjectMapper objectMapper;

    public RedisVectorStore(StringRedisTemplate redisTemplate, EmbeddingProvider embeddingProvider, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.embeddingProvider = embeddingProvider;
        this.objectMapper = objectMapper;
        // 启动时确保索引存在（幂等操作）
        ensureIndexExists();
    }

    @Override
    public void add(List<Document> documents) {
        for (Document doc:documents){
            List<Double> vector = embeddingProvider.embed(doc.getContent());
            Map<String,String> hash = new HashMap<>();
            hash.put("content",doc.getContent());
            hash.put("id",doc.getId());
            hash.put("vector",vectorToString(vector));
            try {
                hash.put("metadata",objectMapper.writeValueAsString(doc.getMetadata()));
            }catch (Exception e){
                hash.put("metadata","{}");
            }
            String key = KEY_PREFIX + doc.getId();
            redisTemplate.opsForHash().putAll(key,hash);
        }
    }

    private void ensureIndexExists() {
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) conn ->
                    conn.execute("FT.CREATE", new byte[][]{
                            INDEX_NAME.getBytes(),
                            "ON".getBytes(), "HASH".getBytes(),
                            "PREFIX".getBytes(), "1".getBytes(), KEY_PREFIX.getBytes(),
                            "SCHEMA".getBytes(),
                            "content".getBytes(), "TEXT".getBytes(),
                            "vector".getBytes(), "VECTOR".getBytes(), "FLAT".getBytes(),
                            "6".getBytes(),
                            "TYPE".getBytes(), "FLOAT32".getBytes(),
                            "DIM".getBytes(), "1536".getBytes(), // 需与 EmbeddingProvider 输出维度一致
                            "DISTANCE_METRIC".getBytes(), "COSINE".getBytes()
                    })
            );
        } catch (Exception e) {
            // 索引已存在时 Redis 会抛出异常，忽略即可（幂等）
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int topK) {
        List<Double> queryVector = embeddingProvider.embed(query);
        String vectorStr = vectorToString(queryVector);
        Object rawResult = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Object>) conn ->
                        conn.execute("FT.SEARCH", buildKnnArgs(INDEX_NAME, vectorStr, topK))
        );
        return parseSearchResult(rawResult);
    }

    private byte[][] buildKnnArgs(String index,String vectorStr,int topK){
        String queryStr = "*=>[KNN " + topK + " @vector $vec AS score]";
        return new byte[][]{
                index.getBytes(),
                queryStr.getBytes(),
                "PARAMS".getBytes(), "2".getBytes(),
                "vec".getBytes(), vectorStr.getBytes(),
                "RETURN".getBytes(), "2".getBytes(),
                "content".getBytes(), "metadata".getBytes(),
                "SORTBY".getBytes(), "score".getBytes(),
                "LIMIT".getBytes(), "0".getBytes(), String.valueOf(topK).getBytes()
        };
    }

    @SuppressWarnings("unchecked")
    private List<Document> parseSearchResult(Object rawResult){
        List<Document> docs = new ArrayList<>();
        if (rawResult instanceof List<?> results && results.size() > 1){
            for (int i=1;i<results.size();i+=2){
                try{
                    List<byte[]> fields = (List<byte[]>) results.get(i + 1);
                    String content = "";
                    Map<String, Object> metadata = new HashMap<>();
                    for (int j = 0; j < fields.size() - 1; j += 2) {
                        String fieldName = new String(fields.get(j));
                        String fieldVal = new String(fields.get(j + 1));
                        if ("content".equals(fieldName)) content = fieldVal;
                        if ("metadata".equals(fieldName)) {
                            metadata = objectMapper.readValue(fieldVal, Map.class);
                        }
                    }
                    docs.add(new Document(content, metadata));
                }catch (Exception ignored){

                }
            }
        }
        return docs;
    }
    /**
     * 把 List<Double> 向量转成空格分隔的字符串，RediSearch 可直接识别
     */
    private String vectorToString(List<Double> vector){
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<vector.size();i++){
            if(i>0) sb.append(" ");
            sb.append(vector.get(i).floatValue());
        }
        return sb.toString();
    }
}
