package org.echoflow.core.rag;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 纯文本文件加载器：支持 .txt / .md 等任意文本格式。
 */
public class TextDocumentLoader implements DocumentLoader{

    @Override
    public List<Document> load(String uri){
        try{
            String content = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
            Document doc = new Document(content, Map.of("source",uri,"type","text"));
            return List.of(doc);
        } catch (IOException e) {
            throw new RuntimeException("[EchoFlow RAG] 读取文本文件失败: " + uri, e);
        }
    }
}
