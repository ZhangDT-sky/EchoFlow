package org.echoflow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "echoflow")
public class EchoFlowProperties {
    private String provider = "deepseek";

    private String apiKey;

    private String baseUrl = "https://api.deepseek.com";

    private String defaultModel = "deepseek-chat";

    private Mcp mcp = new Mcp();
    public Mcp getMcp(){
        return mcp;
    }
    public void setMcp(Mcp mcp){
        this.mcp=mcp;
    }

    public static class Mcp {
        private List<String> servers = new ArrayList<>();
        public List<String> getServers(){
            return servers;
        }
        public void setServers(List<String> servers){
            this.servers = servers;
        }
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
}
