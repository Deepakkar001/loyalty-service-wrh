package com.loyaltyos.onboarding.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoggingWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpInLoggerInterceptor())
            .order(Ordered.LOWEST_PRECEDENCE);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}

