package org.echoflow.core.provider;

import java.util.List;

public interface EmbeddingProvider {
    /**
     * 将一段自然语言文本转化为指定维度的浮点稠密向量（Embedding）。
     *
     * @param text 待向量化的普通文本
     * @return 该文本对应的浮点向量
     */
    List<Double> embed(String text);
}
