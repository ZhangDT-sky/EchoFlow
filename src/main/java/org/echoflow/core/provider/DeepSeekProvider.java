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

public class DeepSeekProvider implements LLMProvider{
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String defaultModel;

    public DeepSeekProvider(EchoFlowProperties properties, ObjectMapper mapper){
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
        Map<String, Object> body = buildRawRequest(request,false);

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
        Map<String,Object> body = buildRawRequest(request, true);

        return webClient.post()
                .uri("chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> !"[DONE]".equals(data.trim()))
                .map(data -> {
                    try{
                        JsonNode jsonNode = mapper.readTree(data);
                        return parseResponse(jsonNode);
                    }catch (Exception e){
                        throw new RuntimeException("Parse SSE data error", e);
                    }
                });
    }

    /**
     * 内部构建请求体
     */
    private Map<String, Object> buildRawRequest(ChatRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() !=null ? request.getModel() : defaultModel);
        body.put("messages", request.getMessages());
        body.put("stream", stream);
        if (request.getTemperature() != null){
            body.put("temperature", request.getTemperature());
        }
        return body;
    }

    /**
     * 解析同步调用的全量 JSON
     */
    private ChatResponse parseResponse(JsonNode jsonNode) {
        ChatResponse response = new ChatResponse();
        if(jsonNode !=null && jsonNode.has("choices")){
            JsonNode messageNode = jsonNode.get("choices").get(0).get("message");
            if (messageNode.has("content")) {
                response.setContent(messageNode.get("content").asText());
            }
        }

        if(jsonNode !=null &&jsonNode.has("usage")){
            JsonNode usageNode = jsonNode.get("usage");
            ChatResponse.Usage usage = new ChatResponse.Usage();
            usage.setPromptTokens(usageNode.get("prompt_tokens").asInt());
            usage.setCompletionTokens(usageNode.get("completion_tokens").asInt());
            usage.setTotalTokens(usageNode.get("total_tokens").asInt());
            response.setUsage(usage);
        }
        return response;
    }

}
