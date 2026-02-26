package org.echoflow.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoflow.aop.TokenLoggingAspect;
import org.echoflow.core.context.ChatMemory;
import org.echoflow.core.context.InMemoryChatMemory;
import org.echoflow.core.context.SlidingWindowStrategy;
import org.echoflow.core.prompt.PromptTemplateEngine;
import org.echoflow.core.provider.DeepSeekProvider;
import org.echoflow.core.provider.LLMProvider;
import org.echoflow.core.proxy.AIServiceScannerRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
    public org.echoflow.core.tool.AIToolRegistry aiToolRegistry() {
        return new org.echoflow.core.tool.AIToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowStrategy slidingWindowStrategy() {
        return new SlidingWindowStrategy(4000);
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
