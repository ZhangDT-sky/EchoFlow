package org.echoflow.test;

import org.echoflow.test.service.ChatBotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

// 这是一个模拟的使用方主程序
@SpringBootApplication(scanBasePackages = { "org.echoflow" })
class MockApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockApplication.class, args);
    }
}

// 核心测试类
@SpringBootTest(classes = MockApplication.class)
public class EchoFlowApplicationTest {

    @Autowired
    private ChatBotService chatBotService;

    @Test
    public void testBasicPrompt() {
        System.out.println("========== 测试单一问答 ==========");
        String response = chatBotService.askArchitect("什么是 Spring Boot Starter？");
        System.out.println("AI 回答：" + response);
    }

    @Test
    public void testContextMemory() {
        System.out.println("\n========== 测试智能记忆与滑动窗口 ==========");
        String sessionId = UUID.randomUUID().toString();

        System.out.println("我: 请记住，我最喜欢的语言是 Java。");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "请记住，我最喜欢的语言是 Java。"));

        System.out.println("\n我: 我昨天刚学了 Python。");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "我昨天刚学了 Python。"));

        System.out.println("\n我: 请问我最喜欢的语言是什么？");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "请问我最喜欢的语言是什么？"));
    }
}
