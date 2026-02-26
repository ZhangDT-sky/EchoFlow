package org.echoflow.core.proxy;

import org.echoflow.annotation.AIToolParam;
import org.echoflow.annotation.Prompt;
import org.echoflow.annotation.V;
import org.echoflow.core.chat.ChatRequest;
import org.echoflow.core.chat.Message;
import org.echoflow.core.context.ChatMemory;
import org.echoflow.core.context.SlidingWindowStrategy;
import org.echoflow.core.prompt.PromptTemplateEngine;
import org.echoflow.core.provider.LLMProvider;
import org.echoflow.core.tool.AIToolRegistry;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AIServiceInvocationHandler implements InvocationHandler {

    private final Class<?> interfaceClass;
    private final LLMProvider llmProvider;
    private final PromptTemplateEngine promptTemplateEngine;
    private final ChatMemory chatMemory;
    private final SlidingWindowStrategy windowStrategy;
    private final ObjectMapper objectMapper;
    private final AIToolRegistry toolRegistry;

    public AIServiceInvocationHandler(Class<?> interfaceClass, LLMProvider llmProvider,
            PromptTemplateEngine promptTemplateEngine,
            ChatMemory chatMemory, SlidingWindowStrategy windowStrategy,
            ObjectMapper objectMapper, AIToolRegistry toolRegistry) {
        this.interfaceClass = interfaceClass;
        this.llmProvider = llmProvider;
        this.promptTemplateEngine = promptTemplateEngine;
        this.chatMemory = chatMemory;
        this.windowStrategy = windowStrategy;
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }

        // 关键改动：通过接口的类找到原本的方法对象，这样才能拿到接口上的特定注解
        Method actualMethod = interfaceClass.getMethod(method.getName(), method.getParameterTypes());

        Prompt promptAnnotation = actualMethod.getAnnotation(Prompt.class);
        if (promptAnnotation == null || !StringUtils.hasText(promptAnnotation.value())) {
            throw new IllegalStateException("Method " + method.getName() + " must be annotated with @Prompt");
        }

        // 解析参数
        Map<String, Object> variables = new HashMap<>();
        String currentSessionId = "default-session";
        if (args != null) {
            java.lang.annotation.Annotation[][] parameterAnnotations = actualMethod.getParameterAnnotations();
            for (int i = 0; i < args.length; i++) {
                String varName = null;
                // 遍历这个参数的所有注解，寻找 @V
                for (java.lang.annotation.Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof V) {
                        varName = ((V) annotation).value();
                        break;
                    }
                }
                // 如果没有 @V 注解
                if (varName == null) {
                    varName = "arg" + i;
                }

                variables.put(varName, args[i]);

                if ("sessionId".equals(varName) && args[i] instanceof String) {
                    currentSessionId = (String) args[i];
                }
            }
        }
        // 1. 生成实际 Prompt 并记录为用户消息
        String realPrompt = promptTemplateEngine.render(promptAnnotation.value(), variables);

        // 探查期望的返回类型
        java.lang.reflect.Type genericReturnType = actualMethod.getGenericReturnType();
        Class<?> rawReturnType = actualMethod.getReturnType();
        boolean isString = String.class.equals(rawReturnType);
        boolean isFlux = reactor.core.publisher.Flux.class.isAssignableFrom(rawReturnType);
        boolean isStructuredOutput = !isString && !isFlux;

        if (isStructuredOutput) {
            StringBuilder fieldsInfo = new StringBuilder();
            for (java.lang.reflect.Field f : rawReturnType.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    fieldsInfo.append("  \"").append(f.getName()).append("\": <").append(f.getType().getSimpleName())
                            .append(">,\n");
                }
            }
            String schemaStr = "{\n" + fieldsInfo.toString() + "}";
            // 隐式追加加强的 Prompt 系统约束
            realPrompt += "\n\n系统指令：请严格只返回合法的 JSON 格式数据。不要包含任何前后缀说明，不要输出 ```json 这类 Markdown 标记标签。目标 JSON 对象必须包含以下由系统预定义的字段名：\n"
                    + schemaStr + "\n确保该文本可以直接被程序反序列化为前述结构。";
        }

        Message userMessage = Message.user(realPrompt);
        chatMemory.addMessage(currentSessionId, userMessage);

        // 2. 取出历史记录并执行滑动窗口策略（自动截断过长老消息）
        List<Message> historyMessages = chatMemory.getMessages(currentSessionId);
        List<Message> limitedMessages = windowStrategy.apply(historyMessages);

        // 3. 构建发过去的请求
        ChatRequest request = ChatRequest.of(null, limitedMessages);

        // 将本地方法通过反射组成大模型认识的json schema塞入请求
        if (toolRegistry != null && !toolRegistry.getAllTools().isEmpty()) {
            List<ChatRequest.Tool> tools = new ArrayList<>();
            for (Map.Entry<String, AIToolRegistry.ToolMethodInfo> entry : toolRegistry.getAllTools().entrySet()) {
                ChatRequest.Tool tool = new ChatRequest.Tool();
                ChatRequest.Function function = new ChatRequest.Function();
                function.setName(entry.getKey());
                function.setDescription(entry.getValue().description);

                // 构建 parameters Schema
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("type", "object");
                Map<String, Object> properties = new HashMap<>();

                for (Parameter p : entry.getValue().targetMethod.getParameters()) {
                    Map<String, String> proDef = new HashMap<>();
                    proDef.put("type", "string");
                    AIToolParam param = p.getAnnotation(AIToolParam.class);
                    if (param != null) {
                        proDef.put("description", param.value());
                    }
                    properties.put(p.getName(), proDef);
                }
                parameters.put("properties", properties);
                function.setParameters(parameters);
                tool.setFunction(function);
                tools.add(tool);
            }
            request.setTools(tools);
        }

        // 4. 发送请求并拿到响应
        if (isFlux) {
            // 对流式的特殊处理：目前这里依然简化，因为 Flux 记录完整历史比较麻烦，后续可强化拦截器来记录。
            return llmProvider.streamChat(request).mapNotNull(res -> res.getDelta());
        } else {
            int maxLoop = 5;
            while (maxLoop-- > 0) {
                org.echoflow.core.chat.ChatResponse response = llmProvider.chat(request);

                // [情况 A]：模型正常说了话，没有要求调工具
                if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
                    String content = response.getContent();
                    chatMemory.addMessage(currentSessionId, Message.assistant(content));

                    if (isStructuredOutput) {
                        try {
                            // 轻度清理常见的带有 markdown 语法情况
                            String cleanContent = content.trim();
                            if (cleanContent.startsWith("```json")) {
                                cleanContent = cleanContent.substring(7);
                            } else if (cleanContent.startsWith("```")) {
                                cleanContent = cleanContent.substring(3);
                            }
                            if (cleanContent.endsWith("```")) {
                                cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
                            }
                            cleanContent = cleanContent.trim();

                            System.out.println("[EchoFlow Structured Output] LLM Raw Response:\n" + cleanContent);

                            com.fasterxml.jackson.databind.JavaType javaType = objectMapper.getTypeFactory()
                                    .constructType(genericReturnType);
                            return objectMapper.readValue(cleanContent, javaType);
                        } catch (Exception e) {
                            throw new RuntimeException("EchoFlow 结构化 JSON 反序列化失败! 模型返回内容为: \n" + content, e);
                        }
                    }
                    return content;
                }

                // [情况 B]：大模型要求调用本地工具
                // 1. 记下模型发来的工具调用指令
                Message assistantMsg = Message.assistant(null);
                assistantMsg.setTool_calls(response.getToolCalls());
                chatMemory.addMessage(currentSessionId, assistantMsg);

                // 2. 遍历执行每一个它要求的工具
                for (Message.ToolCall toolCall : response.getToolCalls()) {
                    String callId = toolCall.getId();
                    String funcName = toolCall.getFunction().getName();
                    String jsonArgs = toolCall.getFunction().getArguments();

                    AIToolRegistry.ToolMethodInfo methodInfo = toolRegistry.getAllTools().get(funcName);
                    Object executeResult;
                    try {
                        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(jsonArgs);
                        Object[] javaArgs = new Object[methodInfo.targetMethod.getParameterCount()];

                        for (int k = 0; k < methodInfo.targetMethod.getParameters().length; k++) {
                            Parameter p = methodInfo.targetMethod.getParameters()[k];
                            com.fasterxml.jackson.databind.JsonNode valNode = jsonNode.get(p.getName());
                            javaArgs[k] = (valNode != null) ? objectMapper.treeToValue(valNode, p.getType()) : null;
                        }

                        // 反射执行
                        executeResult = methodInfo.targetMethod.invoke(methodInfo.targetBean, javaArgs);
                        System.out.println("[EchoFlow Agent] 本地工具 " + funcName + " 执行成功，结果：" + executeResult);
                    } catch (Exception e) {
                        System.err.println("[EchoFlow Agent] 本地工具执行失败：" + e.getMessage());
                        executeResult = "内部执行错误：" + e.getMessage();
                    }

                    // 3. 把执行结果封入 Message 发回（role 必须是 tool）
                    String strResult = (executeResult instanceof String) ? (String) executeResult
                            : objectMapper.writeValueAsString(executeResult);
                    chatMemory.addMessage(currentSessionId, Message.tool(callId, strResult));
                }

                // 4. 将加上工具包执行结果的最新记忆列表覆盖 request，继续死循环下一轮发给大模型
                request.setMessages(windowStrategy.apply(chatMemory.getMessages(currentSessionId)));
            }
            throw new RuntimeException("达到工具调用并发极值，强制中断 Agent。");
        }
    }
}
