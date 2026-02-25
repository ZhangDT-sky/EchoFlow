package org.echoflow.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface V {

    /**
     * 绑定的变量名
     */
    String value();
}
