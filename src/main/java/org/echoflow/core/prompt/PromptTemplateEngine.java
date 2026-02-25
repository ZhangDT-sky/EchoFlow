package org.echoflow.core.prompt;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

public class PromptTemplateEngine {
    private final ExpressionParser parser = new SpelExpressionParser();

    // 定义模板占位符的格式为 {{变量名}}
    private final TemplateParserContext templateParserContext = new TemplateParserContext("{{", "}}");

    /**
     * 根据模板字符串和参数上下文，渲染出最终的 Prompt 文本
     *
     * @param template  e.g. "以{{style}}的风格，扩写大纲：{{outline}}"
     * @param variables key 对应变量名, value 对应实际用户传的值
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        StandardEvaluationContext context = new StandardEvaluationContext(variables);
        context.addPropertyAccessor(new org.springframework.context.expression.MapAccessor());
        // 解析表达式
        Expression expression = parser.parseExpression(template, templateParserContext);

        // 使用 String.class 作为期望的返回类型
        return expression.getValue(context, String.class);
    }

}
