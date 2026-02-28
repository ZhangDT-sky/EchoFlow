package org.echoflow.core.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public class McpClient implements InitializingBean {
    private final AIToolRegistry toolRegistry;
    private final List<String> mcpServers;

    public McpClient(AIToolRegistry toolRegistry, List<String> mcpServers) {
        this.toolRegistry = toolRegistry;
        this.mcpServers = mcpServers;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (mcpServers == null||mcpServers.isEmpty()){
            return;
        }
        for (String serverUrl:mcpServers){
            try {
                WebClient client = WebClient.builder().baseUrl(serverUrl).build();
                //
                JsonNode responseNode = client.get()
                        .uri("/mcp/tools/list")
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
                if (responseNode!=null&&responseNode.has("tools")){
                    for (JsonNode toolDef:responseNode.get("tools")){
                        String toolName = toolDef.get("name").asText();
                        String description = toolDef.has("description") ? toolDef.get("description").asText() : "";
                        // 从远程拿到的 inputSchema 强转为 Map (利用 ObjectMapper 或 Jackson API)
                        Map<String,Object> parameterSchema = new ObjectMapper().convertValue(toolDef.get("inputSchema"),Map.class);
                        McpRemoteToolExecutor remoteToolExecutor = new McpRemoteToolExecutor(serverUrl,toolName);

                        //混入本地大池子
                        toolRegistry.addTool(toolName,description,parameterSchema,remoteToolExecutor);
                        System.out.println("[EchoFlow MCP] 远程工具拉取并挂载成功 \uD83D\uDD2C: " + toolName);
                    }
                }
            }catch (Exception e){
                System.err.println("[EchoFlow MCP] 无法连接到远程上下文服务器 " + serverUrl + "，该节点工具将缺失。");
            }
        }
    }
}
