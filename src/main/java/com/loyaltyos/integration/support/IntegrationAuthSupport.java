package com.loyaltyos.integration.support;

import com.loyaltyos.integration.security.ApiKeyPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

public final class IntegrationAuthSupport {

    private IntegrationAuthSupport() {}

    public static void verifyTenant(ApiKeyPrincipal auth, String tenantId) {
        if (auth == null) {
            throw new AccessDeniedException("API key authentication required");
        }
        if (!tenantId.equals(auth.tenantId())) {
            throw new AccessDeniedException(
                "Tenant mismatch: URL tenantId does not match API key tenant");
        }
    }

    public static String attributeBody(HttpServletRequest request) {
        Object body = request.getAttribute("integration.requestBody");
        return body != null ? String.valueOf(body) : "";
    }

    public static String requestId(HttpServletRequest request) {
        Object id = request.getAttribute("integration.requestId");
        return id != null ? String.valueOf(id) : java.util.UUID.randomUUID().toString();
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
