package org.echoflow.core.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoflow.autoconfigure.EchoFlowProperties;
import org.echoflow.core.chat.ChatRequest;
import org.echoflow.core.chat.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.Map;

public class DeepSeekProvider implements LLMProvider {
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String defaultModel;

    public DeepSeekProvider(EchoFlowProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.defaultModel = properties.getDefaultModel();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Map<String, Object> body = buildRawRequest(request, false);

        // 发起同步请求
        JsonNode jsonNode = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(); // block 阻塞等待结果
        return parseResponse(jsonNode);
    }

    @Override
    public Flux<ChatResponse> streamChat(ChatRequest request) {
        Map<String, Object> body = buildRawRequest(request, true);

        return webClient.post()
                .uri("chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> !"[DONE]".equals(data.trim()))
                .map(data -> {
                    try {
                        JsonNode jsonNode = mapper.readTree(data);
                        return parseResponse(jsonNode);
                    } catch (Exception e) {
                        throw new RuntimeException("Parse SSE data error", e);
                    }
                });
    }

    /**
     * 内部构建请求体
     */
    private Map<String, Object> buildRawRequest(ChatRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        body.put("messages", request.getMessages());
        body.put("stream", stream);
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        // 如果有工具，把工具列表塞入请求
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }
        return body;
    }

    /**
     * 解析同步调用的全量 JSON
     */
    private ChatResponse parseResponse(JsonNode jsonNode) {
        try {
            // 利用 ObjectMapper 直接映射整个对象结构
            RawResponse raw = mapper.treeToValue(jsonNode, RawResponse.class);
            ChatResponse response = new ChatResponse();

            if (raw.choices != null && !raw.choices.isEmpty()) {
                RawResponse.Choice choice = raw.choices.get(0);
                if (choice.message != null) {
                    response.setContent(choice.message.content);
                    response.setToolCalls(choice.message.toolCalls);
                }
            }

            if (raw.usage != null) {
                ChatResponse.Usage usage = new ChatResponse.Usage();
                usage.setPromptTokens(raw.usage.promptTokens);
                usage.setCompletionTokens(raw.usage.completionTokens);
                usage.setTotalTokens(raw.usage.totalTokens);
                response.setUsage(usage);
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("解析模型返回内容失败, 原始内容为: " + jsonNode.toString(), e);
        }
    }

    /**
     * 对齐 OpenAI / DeepSeek API 结构的内部 DTO
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawResponse {
        public java.util.List<Choice> choices;
        public Usage usage;

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            public Message message;
        }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            public String content;
            @com.fasterxml.jackson.annotation.JsonProperty("tool_calls")
            public java.util.List<org.echoflow.core.chat.Message.ToolCall> toolCalls;
        }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Usage {
            @com.fasterxml.jackson.annotation.JsonProperty("prompt_tokens")
            public int promptTokens;
            @com.fasterxml.jackson.annotation.JsonProperty("completion_tokens")
            public int completionTokens;
            @com.fasterxml.jackson.annotation.JsonProperty("total_tokens")
            public int totalTokens;
        }
    }
}
