package com.loyaltyos.onboarding.rules.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(RulesProperties.class)
public class RuleEngineConfiguration {

    @Bean
    public ExpressionParser spelExpressionParser() {
        return new SpelExpressionParser();
    }

    /**
     * Bounded pool for time-bounded SpEL evaluation (see {@code loyalty.rules.evaluation-timeout-ms}).
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor ruleSpelExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("rule-spel-");
        ex.initialize();
        return ex;
    }
}
