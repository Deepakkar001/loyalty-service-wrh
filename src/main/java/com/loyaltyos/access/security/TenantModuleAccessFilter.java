package com.loyaltyos.access.security;



import com.fasterxml.jackson.databind.ObjectMapper;

import com.loyaltyos.onboarding.security.TenantJwt;

import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;



import java.io.IOException;

import java.util.Map;



@Component

public class TenantModuleAccessFilter extends OncePerRequestFilter {



    private final ModuleEntitlementGuard moduleEntitlementGuard;

    private final ObjectMapper objectMapper;



    public TenantModuleAccessFilter(ModuleEntitlementGuard moduleEntitlementGuard, ObjectMapper objectMapper) {

        this.moduleEntitlementGuard = moduleEntitlementGuard;

        this.objectMapper = objectMapper;

    }



    @Override

    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        if (path == null) {

            return true;

        }

        return path.startsWith("/api/v1/auth/")

            || path.startsWith("/api/v1/admin/")

            || path.startsWith("/api/v1/merchant/")

            || path.startsWith("/api/v1/integration/")

            || path.startsWith("/api/v1/onboarding/")

            || path.startsWith("/api-docs/")

            || path.startsWith("/swagger-ui")

            || path.startsWith("/actuator/");

    }



    @Override

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)

        throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {

            filterChain.doFilter(request, response);

            return;

        }

        Jwt jwt = jwtAuth.getToken();

        if (TenantJwt.isMerchant(jwt)) {

            filterChain.doFilter(request, response);

            return;

        }

        String role = TenantJwt.role(jwt);

        if (role != null && role.startsWith("PLATFORM")) {

            filterChain.doFilter(request, response);

            return;

        }



        String tenantId = TenantJwt.tenantId(jwt);

        if (tenantId == null || tenantId.isBlank()) {

            filterChain.doFilter(request, response);

            return;

        }



        String path = request.getRequestURI();

        var moduleKey = TenantModuleApiResolver.resolveModuleKey(path);

        if (moduleKey.isEmpty()) {

            filterChain.doFilter(request, response);

            return;

        }



        try {

            moduleEntitlementGuard.requireModule(tenantId, moduleKey.get());

            filterChain.doFilter(request, response);

        } catch (com.loyaltyos.access.exception.ModuleNotEntitledException ex) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            objectMapper.writeValue(response.getOutputStream(), Map.of(

                "code", "MODULE_NOT_ENTITLED",

                "message", ex.getMessage(),

                "moduleKey", ex.getModuleKey()

            ));

        }

    }

}


