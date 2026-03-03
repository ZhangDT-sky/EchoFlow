package org.echoflow.core.rag;

import java.util.List;

/**
 * 文档加载器接口：将外部资源（本地文件、URL）读取并转换为标准 Document 列表。
 * 策略模式设计，不同格式由不同的实现类承载。
 */
public interface DocumentLoader {
    List<Document> load(String url);
}

