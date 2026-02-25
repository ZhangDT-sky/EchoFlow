package org.echoflow.annotation;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Prompt {

    /**
     * 提示词模板内容，如 "请用{{style}}风格扩写：{{outline}}"
     */
    String value();
}