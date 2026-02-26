package org.echoflow.core.proxy;

import org.echoflow.annotation.Prompt;
import org.echoflow.annotation.V;
import org.echoflow.core.chat.ChatRequest;
import org.echoflow.core.chat.Message;
import org.echoflow.core.context.ChatMemory;
import org.echoflow.core.context.SlidingWindowStrategy;
import org.echoflow.core.prompt.PromptTemplateEngine;
import org.echoflow.core.provider.LLMProvider;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
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

    public AIServiceInvocationHandler(Class<?> interfaceClass, LLMProvider llmProvider,
            PromptTemplateEngine promptTemplateEngine,
            ChatMemory chatMemory, SlidingWindowStrategy windowStrategy,
            ObjectMapper objectMapper) {
        this.interfaceClass = interfaceClass;
        this.llmProvider = llmProvider;
        this.promptTemplateEngine = promptTemplateEngine;
        this.chatMemory = chatMemory;
        this.windowStrategy = windowStrategy;
        this.objectMapper = objectMapper;
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

        // 4. 发送请求并拿到响应
        if (isFlux) {
            // 对流式的特殊处理：目前这里依然简化，因为 Flux 记录完整历史比较麻烦，后续可强化拦截器来记录。
            return llmProvider.streamChat(request).mapNotNull(res -> res.getDelta());
        } else {
            // 同步响应：将 AI 返回的话存入内存
            String content = llmProvider.chat(request).getContent();
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
    }
}
