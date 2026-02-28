package org.echoflow.core.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 本地 Java 方法的执行包装器。
 * 兼容原有 @AITool 注解所标注的本地 Bean 方法的反射调用逻辑。
 */
public class LocalMethodToolExecutor implements ToolExecutor {

    private final Object targetBean;
    private final Method targetMethod;
    private final ObjectMapper objectMapper;

    public LocalMethodToolExecutor(Object targetBean, Method targetMethod, ObjectMapper objectMapper) {
        this.targetBean = targetBean;
        this.targetMethod = targetMethod;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object execute(JsonNode args) throws Exception {
        Object[] javaArgs = new Object[targetMethod.getParameterCount()];
        Parameter[] parameters = targetMethod.getParameters();

        for (int k = 0; k < parameters.length; k++) {
            Parameter p = parameters[k];
            JsonNode valNode = args != null ? args.get(p.getName()) : null;
            javaArgs[k] = (valNode != null && !valNode.isNull())
                    ? objectMapper.treeToValue(valNode, p.getType())
                    : null;
        }

        // 强行突破访问限制（防御性，处理非 public 方法）
        targetMethod.setAccessible(true);
        return targetMethod.invoke(targetBean, javaArgs);
    }
}
