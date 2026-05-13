package com.loyaltyos.onboarding.logging;

import com.loyaltyos.onboarding.security.TenantJwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds a per-request correlation id (traceId) to MDC, and propagates it as a response header.
 * Also attempts to attach tenantId to MDC when JWT is present.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_TENANT_ID = "tenantId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String incoming = request.getHeader(HEADER_REQUEST_ID);
        String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();

        MDC.put(MDC_TRACE_ID, traceId);

        // Attach tenantId when authenticated.
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jat) {
                String tenantId = TenantJwt.tenantId(jat.getToken());
                if (tenantId != null && !tenantId.isBlank()) MDC.put(MDC_TENANT_ID, tenantId);
            }
        } catch (Exception ignored) {
            // Best-effort only
        }

        response.setHeader(HEADER_REQUEST_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_TENANT_ID);
        }
    }
}

