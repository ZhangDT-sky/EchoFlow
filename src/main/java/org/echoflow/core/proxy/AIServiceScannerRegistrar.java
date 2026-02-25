package org.echoflow.core.proxy;

import org.echoflow.annotation.AIService;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Set;

public class AIServiceScannerRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 创建一个扫描器，但不使用默认的 Filter (因为接口默认不被当做 Component)
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                // 允许顶层接口成为候选组件
                return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
            }
        };
        // 告诉扫描器，我们要找打了 @AIService 注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(AIService.class));
        // 我们默认扫描框架基础包及其子包，同时也允许扫当前装配类所在的包
        String basePackage = "org.echoflow";

        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
        for (BeanDefinition candidateComponent : candidateComponents) {
            String beanClassName = candidateComponent.getBeanClassName();
            Class<?> clazz;
            try {
                clazz = Class.forName(beanClassName);
            } catch (ClassNotFoundException e) {
                continue;
            }
            // 用 FactoryBean 替换原有的 BeanDefinition
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AIServiceFactoryBean.class);
            builder.addConstructorArgValue(clazz);
            // 按照类型注入依赖 (Spring 容器会自动把 LLMProvider 和 TemplateEngine 塞进 FactoryBean)
            builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

            String beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0))
                    + clazz.getSimpleName().substring(1);
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        }
    }
}
