package org.echoflow.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoflow.aop.TokenLoggingAspect;
import org.echoflow.core.context.*;
import org.echoflow.core.prompt.PromptTemplateEngine;
import org.echoflow.core.provider.DeepSeekProvider;
import org.echoflow.core.provider.LLMProvider;
import org.echoflow.core.proxy.AIServiceScannerRegistrar;
import org.echoflow.core.tool.AIToolRegistry;
import org.echoflow.core.tool.McpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(EchoFlowProperties.class)
@Import(AIServiceScannerRegistrar.class) // 触发自定义 @AIService 注解的扫描
public class EchoFlowAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public PromptTemplateEngine promptTemplateEngine() {
        return new PromptTemplateEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public org.echoflow.core.tool.AIToolRegistry aiToolRegistry(ObjectMapper objectMapper) {
        AIToolRegistry registry = new AIToolRegistry();
        registry.setObjectMapper(objectMapper);
        return registry;
    }

    public McpClient mcpClient(
            AIToolRegistry registry,
            EchoFlowProperties properties
    ){
        System.out.println("[EchoFlow Config] 检测到 MCP 配置，启动远端模型上下文服务器客户端 (McpClient)");
        return new org.echoflow.core.tool.McpClient(registry, properties.getMcp().getServers());
    }


    // =========== 分支一：如果检测到了 Redis ============
    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "echoflow.memory", name = "type", havingValue = "redis")
    public ChatMemory redisChatMemory(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        System.out.println("[EchoFlow Config] 启动分布式持久化记忆 (RedisChatMemory)");
        return new RedisChatMemory(redisTemplate, objectMapper);
    }

    // =========== 分支二：兜底使用内存模式 ============
    @Bean
    @ConditionalOnMissingBean
    public ChatMemory chatMemory() {
        System.out.println("[EchoFlow Config] 启动单机内存记忆 (InMemoryChatMemory)");
        return new InMemoryChatMemory();
    }

    @Bean
    @ConditionalOnProperty(prefix = "echoflow.memory", name = "strategy", havingValue = "summary")
    public SlidingWindowStrategy summaryWindowStrategy(LLMProvider llmProvider) {
        System.out.println("[EchoFlow Config] 启动大模型隐式摘要上下文窗口 (SummaryWindowStrategy)");
        return new SummaryWindowStrategy(8000, llmProvider, 10);
    }

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowStrategy slidingWindowStrategy() {
        System.out.println("[EchoFlow Config] 启动普通滑动窗口截断策略 (SlidingWindowStrategy)");
        return new SlidingWindowStrategy(8000);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "echoflow", name = "provider", havingValue = "deepseek", matchIfMissing = true)
    public LLMProvider deepSeekProvider(EchoFlowProperties properties, ObjectMapper objectMapper) {
        return new DeepSeekProvider(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenLoggingAspect tokenLoggingAspect() {
        return new TokenLoggingAspect();
    }
}
