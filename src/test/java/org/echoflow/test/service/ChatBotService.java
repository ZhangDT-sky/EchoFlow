package org.echoflow.test.service;

import org.echoflow.annotation.AIService;
import org.echoflow.annotation.LogToken;
import org.echoflow.annotation.Prompt;
import org.echoflow.annotation.V;

@AIService
public interface ChatBotService {

    // 1. 无记忆、纯模板测试
    @LogToken
    @Prompt("请你扮演一名资深 Java 架构师，用不超过50个字简单回答：{{question}}")
    String askArchitect(@V("question") String question);

    // 2. 带记忆测试（关键所在：只要传相同 sessionId 就能记住上下文）
    @LogToken
    @Prompt("你叫小伊。{{input}}")
    String chatWithMemory(@V("sessionId") String sessionId, @V("input") String input);
}
