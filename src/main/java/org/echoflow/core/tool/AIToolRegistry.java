package org.echoflow.core.tool;

import org.echoflow.annotation.AITool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AIToolRegistry implements BeanPostProcessor {

    private final Map<String, ToolMethodInfo> toolRegistry = new ConcurrentHashMap<>();

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            AITool aiTool = method.getAnnotation(AITool.class);
            if (aiTool != null) {
                String toolName = aiTool.name().isEmpty() ? method.getName() : aiTool.name();
                ToolMethodInfo toolInfo = new ToolMethodInfo(bean, method, aiTool.description());
                toolRegistry.put(toolName, toolInfo);
                System.out.println("[EchoFlow Agent] 工具注册成功: " + toolName);
            }
        }
        return bean;
    }

    public Map<String, ToolMethodInfo> getAllTools() {
        return toolRegistry;
    }

    public static class ToolMethodInfo {
        public Object targetBean;
        public Method targetMethod;
        public String description;

        public ToolMethodInfo(Object bean, Method method, String description) {
            this.targetBean = bean;
            this.targetMethod = method;
            this.description = description;
        }
    }
}
