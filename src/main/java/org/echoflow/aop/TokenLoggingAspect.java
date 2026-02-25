package org.echoflow.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

@Aspect
public class TokenLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(TokenLoggingAspect.class);

    @Around("@annotation(org.echoflow.annotation.LogToken)")
    public Object logTokenUsage(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            // 计算 Token 大致消耗
            int inputTokens = 0;
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof String) {
                    inputTokens += org.echoflow.utils.TokenUtils.estimateTokens((String) arg);
                }
            }
            int outputTokens = (result instanceof String)
                    ? org.echoflow.utils.TokenUtils.estimateTokens((String) result)
                    : 0;

            log.info(
                    "[EchoFlow Audit] Method: {} | Status: Success | Latency: {}ms | InputTokens(Est): {} | OutputTokens(Est): {}",
                    methodName, costTime, inputTokens, outputTokens);
            return result;
        } catch (Throwable e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("[EchoFlow Audit] Method: {} | Status: Failed | Latency: {}ms | Error: {}", methodName, costTime,
                    e.getMessage());
            throw e;
        }
    }
}
