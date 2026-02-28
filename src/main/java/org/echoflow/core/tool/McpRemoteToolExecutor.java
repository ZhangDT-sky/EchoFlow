package org.echoflow.core.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * 远程 MCP 服务的工具执行包装器。将大模型要求调用的工具参数通过 HTTP 扔给远端 Python/Node 服务器处理。
 */
public class McpRemoteToolExecutor implements ToolExecutor{

    private final String mcpServerUrl;
    private final String remoteToolName;
    private final WebClient webClient;

    public McpRemoteToolExecutor(String mcpServerUrl, String remoteToolName) {
        this.mcpServerUrl = mcpServerUrl;
        this.remoteToolName = remoteToolName;
        this.webClient = WebClient.builder().baseUrl(mcpServerUrl).build();
    }


    @Override
    public Object execute(JsonNode args) throws Exception {
        System.out.println();
        Map<String,Object> requestBody = Map.of(
                "jsonrpc","2.0",
                "method","tools/call",
                "params",Map.of(
                        "name",remoteToolName,
                        "arguments",args
                ),
                "id", UUID.randomUUID().toString()
        );
        return webClient.post()
                .uri("/mcp/call")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
