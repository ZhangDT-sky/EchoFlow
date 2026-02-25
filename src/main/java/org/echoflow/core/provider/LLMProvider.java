package org.echoflow.core.provider;

import org.echoflow.core.chat.ChatRequest;
import org.echoflow.core.chat.ChatResponse;
import reactor.core.publisher.Flux;

public interface LLMProvider {

    /**
     * 同步非流式对话
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 响应式流式对话（SSE）
     */
    Flux<ChatResponse> streamChat(ChatRequest request);

}
