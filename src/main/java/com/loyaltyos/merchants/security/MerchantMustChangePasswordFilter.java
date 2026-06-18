package com.loyaltyos.merchants.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.security.TenantJwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MerchantMustChangePasswordFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public MerchantMustChangePasswordFilter(ObjectMapper objectMapper) {
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
        if (!path.startsWith("/api/v1/merchant/")) {
            return true;
        }
        return path.equals("/api/v1/merchant/auth/login")
            || path.equals("/api/v1/merchant/auth/change-password")
            || path.startsWith("/api/v1/merchant/invite/");
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
        if (!TenantJwt.isMerchant(jwt) || !TenantJwt.mustChangePassword(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
            "status", 403,
            "error", "PASSWORD_CHANGE_REQUIRED",
            "message", "You must change your password before continuing.",
            "path", request.getRequestURI()
        ));
    }
}
