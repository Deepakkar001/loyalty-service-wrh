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
import java.util.Objects;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (path.equals("/api/v1/auth/change-password")
            || path.equals("/api/v1/auth/logout")
            || path.equals("/api/v1/auth/refresh")) {
            return true;
        }
        return path.startsWith("/api/v1/auth/")
            || path.startsWith("/api/v1/admin/")
            || path.startsWith("/api/v1/merchant/")
            || path.startsWith("/api/v1/integration/")
            || path.startsWith("/api/v1/onboarding/register")
            || path.startsWith("/api/v1/onboarding/metadata")
            || path.startsWith("/api/v1/onboarding/verify-email")
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
        if (!TenantJwt.mustChangePassword(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
            "status", 403,
            "error", "PASSWORD_CHANGE_REQUIRED",
            "message", "You must change your temporary password before continuing.",
            "path", request.getRequestURI()
        ));
    }
}
