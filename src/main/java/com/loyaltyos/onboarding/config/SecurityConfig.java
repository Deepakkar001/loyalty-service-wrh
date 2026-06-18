package com.loyaltyos.onboarding.config;

import com.loyaltyos.access.security.MustChangePasswordFilter;
import com.loyaltyos.access.security.TenantModuleAccessFilter;
import com.loyaltyos.access.security.TenantSessionVersionFilter;
import com.loyaltyos.merchants.security.MerchantMustChangePasswordFilter;
import com.loyaltyos.onboarding.security.ApiKeyAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final TenantModuleAccessFilter tenantModuleAccessFilter;
    private final TenantSessionVersionFilter tenantSessionVersionFilter;
    private final MustChangePasswordFilter mustChangePasswordFilter;
    private final MerchantMustChangePasswordFilter merchantMustChangePasswordFilter;

    public SecurityConfig(
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
        TenantModuleAccessFilter tenantModuleAccessFilter,
        TenantSessionVersionFilter tenantSessionVersionFilter,
        MustChangePasswordFilter mustChangePasswordFilter,
        MerchantMustChangePasswordFilter merchantMustChangePasswordFilter
    ) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.tenantModuleAccessFilter = tenantModuleAccessFilter;
        this.tenantSessionVersionFilter = tenantSessionVersionFilter;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
        this.merchantMustChangePasswordFilter = merchantMustChangePasswordFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain integrationFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/integration/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .addFilterAfter(tenantModuleAccessFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
            .addFilterAfter(tenantSessionVersionFilter, TenantModuleAccessFilter.class)
            .addFilterAfter(mustChangePasswordFilter, TenantSessionVersionFilter.class)
            .addFilterAfter(merchantMustChangePasswordFilter, MustChangePasswordFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/sign-in",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout",
                    "/api/v1/auth/accept-invite",
                    "/api/v1/merchant/auth/login",
                    "/api/v1/merchant/invite/**",
                    "/api/v1/admin/auth/login",
                    "/api/v1/onboarding/register",
                    "/api/v1/onboarding/metadata",
                    "/api/v1/onboarding/verify-email",
                    "/api/v1/onboarding/verify-email-code",
                    "/api/v1/onboarding/resend-verification",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/admin/**")
                    .hasAnyAuthority("ROLE_PLATFORM_ADMIN", "ROLE_COMPLIANCE_OFFICER")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RoleClaimConverter());
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
            "Location", "Content-Disposition", "X-Request-ID", "X-Processing-Time",
            "X-Rate-Limit-Limit", "X-Rate-Limit-Remaining", "X-Rate-Limit-Reset",
            "Retry-After", "X-Error-ID"
        ));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Extracts the "role" claim from the JWT and maps it to a Spring Security
     * GrantedAuthority with ROLE_ prefix (e.g. "PLATFORM_ADMIN" -> "ROLE_PLATFORM_ADMIN").
     */
    private static class RoleClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(@org.springframework.lang.NonNull Jwt jwt) {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        }
    }
}

