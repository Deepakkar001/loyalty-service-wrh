package com.loyaltyos.access.config;

import com.loyaltyos.access.security.AccessPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    private final AccessPermissionEvaluator permissionEvaluator;

    public MethodSecurityConfig(AccessPermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(new org.springframework.security.access.PermissionEvaluator() {
            @Override
            public boolean hasPermission(
                org.springframework.security.core.Authentication authentication,
                Object targetDomainObject,
                Object permission
            ) {
                return permissionEvaluator.hasPermission(authentication, String.valueOf(permission));
            }

            @Override
            public boolean hasPermission(
                org.springframework.security.core.Authentication authentication,
                java.io.Serializable targetId,
                String targetType,
                Object permission
            ) {
                return permissionEvaluator.hasPermission(authentication, String.valueOf(permission));
            }
        });
        return handler;
    }
}
