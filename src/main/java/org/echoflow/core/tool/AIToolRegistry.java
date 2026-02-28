package org.echoflow.core.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoflow.annotation.AITool;
import org.echoflow.annotation.AIToolParam;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AIToolRegistry implements BeanPostProcessor {

    private final Map<String, ToolInfo> toolRegistry = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper;

    public void setObjectMapper(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            AITool aiTool = method.getAnnotation(AITool.class);
            if (aiTool != null) {
                String toolName = aiTool.name().isEmpty() ? method.getName() : aiTool.name();
                Map<String,Object> parameters = new HashMap<>();
                parameters.put("type","object");
                Map<String,Object> properties = new HashMap<>();
                for (Parameter p : method.getParameters()){
                    Map<String,String> proDef = new HashMap<>();
                    proDef.put("type","string");
                    AIToolParam param = p.getAnnotation(AIToolParam.class);
                    if (param!=null){
                        proDef.put("description",param.value());
                    }
                    properties.put(p.getName(),proDef);
                }
                parameters.put("properties",properties);

                // 包装为本地执行器
                LocalMethodToolExecutor executor = new LocalMethodToolExecutor(bean,method,objectMapper);

                //注册进池子
                this.addTool(toolName, aiTool.description(), parameters, executor);
                System.out.println("[EchoFlow Agent] 工具注册成功: " + toolName);
            }
        }
        return bean;
    }
    // 开放外部注册入口，供 MCP Client 等远端代理使用
    public void addTool(String name, String description, Map<String, Object> parametersSchema, ToolExecutor executor) {
        toolRegistry.put(name, new ToolInfo(name, description, parametersSchema, executor));
    }

    public Map<String, ToolInfo> getAllTools() {
        return toolRegistry;
    }

    /**
     * 弃用
     */
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

    public static class ToolInfo {
        public String name;
        public String description;
        public Map<String,Object> parametersSchema;
        public ToolExecutor executor; //可能是本地反射，也可能是远程HTTP请求

        public ToolInfo(String name, String description, Map<String, Object> parametersSchema, ToolExecutor executor) {
            this.name = name;
            this.description = description;
            this.parametersSchema = parametersSchema;
            this.executor = executor;
        }
    }
}
