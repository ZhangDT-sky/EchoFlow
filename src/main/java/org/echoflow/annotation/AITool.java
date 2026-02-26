package org.echoflow.annotation;

import java.lang.annotation.*;

/**
 * 标注在 Spring Bean 的某个方法上，使其成为一个可供 AI 调用的 Agent Tool
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AITool {
    String name() default ""; //名称
    String description(); //描述
}
