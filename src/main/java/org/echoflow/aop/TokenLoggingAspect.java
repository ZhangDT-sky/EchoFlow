package org.echoflow.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.echoflow.utils.TokenUtils;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

@Aspect
public class TokenLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(TokenLoggingAspect.class);

    //核心监控度量注册表
    @Autowired(required = false)
    private MeterRegistry meterRegistry;


    @Around("@annotation(org.echoflow.annotation.LogToken)")
    public Object logTokenUsage(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String status = "Success";


        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            // 计算 Token 大致消耗
            int inputTokens = 0;
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof String) {
                    inputTokens += TokenUtils.estimateTokens((String) arg);
                }
            }
            int outputTokens = (result instanceof String)
                    ? TokenUtils.estimateTokens((String) result)
                    : 0;

            // 打印审计日志
            log.info(
                    "[EchoFlow Audit] Method: {} | Status: Success | Latency: {}ms | InputTokens(Est): {} | OutputTokens(Est): {}",
                    methodName, costTime, inputTokens, outputTokens);

            // 【高亮新增】上报 Micrometer 监控平台 (如果业务工程里引了 actuator)
            if (meterRegistry != null) {
                // 指标 1：响应延迟打点
                Timer.builder("echoflow.llm.request.timer")
                        .description("AI Service Method Invocation Latency")
                        .tag("method", methodName)
                        .tag("status", status)
                        .register(meterRegistry)
                        .record(costTime, TimeUnit.MILLISECONDS);
                // 指标 2：Token 消耗累加器
                Counter.builder("echoflow.llm.token.usage")
                        .description("AI Service Token Consumption")
                        .tag("method", methodName)
                        .tag("type", "input")
                        .register(meterRegistry)
                        .increment(inputTokens);
                Counter.builder("echoflow.llm.token.usage")
                        .tag("method", methodName)
                        .tag("type", "output")
                        .register(meterRegistry)
                        .increment(outputTokens);
            }
            return result;
        } catch (Throwable e) {
            status = "Failed";
            long costTime = System.currentTimeMillis() - startTime;
            log.error("[EchoFlow Audit] Method: {} | Status: {} | Latency: {}ms | Error: {}", methodName, status, costTime, e.getMessage());
            // 即使失败也要上报慢查询和失败次数
            if (meterRegistry != null) {
                Timer.builder("echoflow.llm.request.timer")
                        .tag("method", methodName)
                        .tag("status", status)
                        .register(meterRegistry)
                        .record(costTime, TimeUnit.MILLISECONDS);
            }
            throw e;
        }
    }
}
