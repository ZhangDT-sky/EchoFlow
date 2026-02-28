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

    @Autowired(required = false)
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Autowired
    private org.echoflow.core.rag.VectorStore vectorStore;

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

    @Test
    public void testMicrometerMetrics() {
        System.out.println("\n========== 测试全链路监控指标暴露 (Micrometer) ==========");
        if (meterRegistry == null) {
            System.err.println("当前测试环境未注入 MeterRegistry，可能缺少 spring-boot-starter-actuator，跳过验证。");
            return;
        }

        // 1. 发起一次常规大模型调用，触发 TokenLoggingAspect 拦截器
        System.out.println("向 LLM 发送请求触发监控打点...");
        chatBotService.askArchitect("请用10个字简要说明什么是 Java 接口。");

        // 2. 从注册表(Registry)里拉取监控计量器
        io.micrometer.core.instrument.Timer timer = meterRegistry.find("echoflow.llm.request.timer")
                .tag("method", "askArchitect")
                .timer();
        io.micrometer.core.instrument.Counter inputCounter = meterRegistry.find("echoflow.llm.token.usage")
                .tag("method", "askArchitect")
                .tag("type", "input")
                .counter();
        io.micrometer.core.instrument.Counter outputCounter = meterRegistry.find("echoflow.llm.token.usage")
                .tag("method", "askArchitect")
                .tag("type", "output")
                .counter();

        System.out.println("==== 提取到的打点核心度量展示 ====");
        System.out.println("大模型 Timer 打点频次：" + (timer != null ? timer.count() : "null"));
        System.out.println("大模型 Timer 总计耗时(ms)："
                + (timer != null ? timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) : "null"));
        System.out.println("大模型 InputToken 预估消耗：" + (inputCounter != null ? inputCounter.count() : "null"));
        System.out.println("大模型 OutputToken 预估消耗：" + (outputCounter != null ? outputCounter.count() : "null"));

        org.junit.jupiter.api.Assertions.assertNotNull(timer, "Timer 不能为 null，说明 Aspect 没有成功将数据刷入 Micrometer。");
        org.junit.jupiter.api.Assertions.assertTrue(timer.count() > 0, "Timer 测试频次应该大于 0");
        org.junit.jupiter.api.Assertions.assertNotNull(inputCounter, "Input Counter 监控打点丢失");
        org.junit.jupiter.api.Assertions.assertTrue(inputCounter.count() > 0, "消耗的 InputTokens 应该大于 0");
    }

    @Test
    public void testRAG() {
        System.out.println("\n========== 测试知识大脑：检索增强生成 (RAG) ==========");

        // 1. 模拟管理员或爬虫把知识塞进向量库
        System.out.println("[管理员] 正在将公司私有规章制度进行 Embedding 向量化入库...");
        java.util.List<org.echoflow.core.rag.Document> internalKnowledge = java.util.Arrays.asList(
                new org.echoflow.core.rag.Document("公司规定：每周五下午3点以后为下午茶时间，允许员工提前一小时也就是下午5点下班。"),
                new org.echoflow.core.rag.Document("报销流程：所有超过 500 元的报销必须经过部门总监审批，并且需要附上纸质发票原件。"),
                new org.echoflow.core.rag.Document("关于年假：入职满一年的员工每年享有额外 5 天的带薪年假，未休完的年假可在次年一月底前折算成工资。"));
        vectorStore.add(internalKnowledge);
        System.out.println("[管理员] 知识片段入库完成！");

        // 2. 模拟普通员工咨询，直接调用 @RAG 强化的接口
        String trickyQuestion = "我刚入司三年，想问问周五下班点是啥时候？另外超过两百块钱的报销需要总监批吗？";
        System.out.println("\n[员工提问]: " + trickyQuestion);

        String answer = chatBotService.askCompanyPolicy(trickyQuestion);
        System.out.println("\n[AI HR大模型回答]:\n" + answer);
    }
}
