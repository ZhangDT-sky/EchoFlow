package org.echoflow.core.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 滑动窗口文本切分器：将超长文档切分为固定大小的 Chunk，并支持首尾重叠（Overlap）防止关键语境丢失。
 * chunkSize: 每个 Chunk 的最大字符数
 * overlap:   相邻 Chunk 末尾与下一个 Chunk 开头的重叠字符数
 */
public class TokenTextSplitter {
    private final int chunkSize;
    private final int overlap;

    public TokenTextSplitter(int chunkSize,int overlap){
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    /** 使用默认参数：每块 500 字，重叠 50 字 */
    public TokenTextSplitter() {
        this(500, 50);
    }

    public List<Document> split(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc:documents){
            result.addAll(splitSingle(doc));
        }
        return result;
    }

    public List<Document> splitSingle(Document doc){
        List<Document> chunks = new ArrayList<>();
        String text = doc.getContent();
        int start = 0;
        int chunkIndex = 0;
        while(start<text.length()){
            int end = Math.min(start + chunkSize, text.length());
            String chunkText = text.substring(start,end);

            Map<String,Object> meta = new HashMap<>(doc.getMetadata());
            meta.put("chunk_index", chunkIndex++);
            meta.put("chunk_start", start);
            chunks.add(new Document(chunkText, meta));
            // 下一个 Chunk 回退 overlap 个字符保证上下文连续
            start += (chunkSize - overlap);
        }
        return chunks;
    }

}
