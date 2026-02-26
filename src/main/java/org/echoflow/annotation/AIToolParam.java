package org.echoflow.annotation;

import java.lang.annotation.*;

/**
 * 标注在 @AITool 方法的参数上，用于生成给大模型看的参数描述 JSON Schema
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AIToolParam {
    String value();    //描述含义
    boolean required() default true;
}

