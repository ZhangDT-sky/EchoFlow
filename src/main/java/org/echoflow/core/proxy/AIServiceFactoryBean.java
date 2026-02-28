package org.echoflow.core.proxy;

import org.echoflow.core.context.ChatMemory;
import org.echoflow.core.context.SlidingWindowStrategy;
import org.echoflow.core.prompt.PromptTemplateEngine;
import org.echoflow.core.provider.LLMProvider;
import org.echoflow.core.tool.AIToolRegistry;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import java.lang.reflect.Proxy;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AIServiceFactoryBean<T> implements FactoryBean<T> {

    private Class<T> mapperInterface;

    @Autowired
    private LLMProvider llmProvider;
    @Autowired
    private PromptTemplateEngine promptTemplateEngine;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private SlidingWindowStrategy windowStrategy;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AIToolRegistry toolRegistry;

    @Autowired
    private org.springframework.core.env.Environment environment;

    public AIServiceFactoryBean() {

    }

    public AIServiceFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public T getObject() throws Exception {
        AIServiceInvocationHandler handler = new AIServiceInvocationHandler(
                mapperInterface, llmProvider, promptTemplateEngine, chatMemory, windowStrategy, objectMapper,
                toolRegistry, environment);

        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[] { mapperInterface },
                handler);
    }

    @Override
    public Class<?> getObjectType() {
        return this.mapperInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<T> getMapperInterface() {
        return mapperInterface;
    }

    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
}
