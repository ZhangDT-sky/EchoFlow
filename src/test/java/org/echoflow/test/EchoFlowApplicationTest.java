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

        System.out.println("我: 请记住，我最喜欢的语言是 C++。");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "请记住，我最喜欢的语言是 C++。"));

        System.out.println("\n我: 我昨天刚学了 Python。");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "我昨天刚学了 Python。"));

        System.out.println("\n我: 请问我最喜欢的语言是什么？");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "请问我最喜欢的语言是什么？"));
    }

    @Test
    public void testStructuredOutput() {
        System.out.println("\n========== 测试结构化 JSON 输出与 Pojo 发绑 ==========");
        org.echoflow.test.entity.UserInfo userInfo = chatBotService.extractUserInfo(
                "我叫张大宝，今年28岁，目前是一名资深的后端研发工程师，我平时喜欢打篮球、听周杰伦的流行音乐以及写代码。");
        System.out.println("解析到的 Pojo 对象：" + userInfo);
        org.junit.jupiter.api.Assertions.assertNotNull(userInfo);
        org.junit.jupiter.api.Assertions.assertEquals("张大宝", userInfo.getName());
    }

    @Test
    public void testSummaryWindow() {
        System.out.println("\n========== 测试高压长会话无损压缩摘要功能 ==========");
        String sessionId = UUID.randomUUID().toString();

        System.out.println("我: 请记住我的核心机密：我的银行卡密码是 123456。");
        System.out.println("AI: " + chatBotService.chatWithMemory(sessionId, "请记住我的核心机密：我的银行卡密码是 123456。"));

        // 故意发送大量废话撑爆 Token (由于我们在打底设置了 8000 Tokens上限，我们发几段超长文字来模拟)
        for (int i = 1; i <= 5; i++) {
            StringBuilder giantNonsense = new StringBuilder("这里是一段毫无意义的重复废话，用来充填并消耗你的上下文资源：");
            for (int k = 0; k < 500; k++) {
                giantNonsense.append("废话");
            }
            giantNonsense.append("。请你随便敷衍我一句。");
            System.out.println("我: [发送了极其漫长的废话第 " + i + " 轮...]");
            chatBotService.chatWithMemory(sessionId, giantNonsense.toString());
        }

        // 此时 Token 一定远远爆表，看看后台大模型是否为老陈的记忆做出了精准摘要
        System.out.println("\n我: 我最初告诉过你一个核心机密，你还记得银行卡密码是多少吗？");
        String finalAnswer = chatBotService.chatWithMemory(sessionId, "我最初告诉过你一个核心机密，你还记得银行卡密码是多少吗？");
        System.out.println("AI: " + finalAnswer);

        // 如果 AI 还能准确答出 123456，说明摘要机制在老对话被抛弃前，完美地把核心机密抽取成了系统消息保存了下来！
    }
}
