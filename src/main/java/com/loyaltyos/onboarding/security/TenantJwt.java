package com.loyaltyos.onboarding.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class TenantJwt {
    private TenantJwt() {}

    public static String tenantId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object v = jwt.getClaims().get("tenantId");
        return v == null ? null : v.toString();
    }

    public static String email(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object v = jwt.getClaims().get("email");
        return v == null ? null : v.toString();
    }

    public static String role(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object v = jwt.getClaims().get("role");
        return v == null ? null : v.toString();
    }

    public static String tenantUserId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object v = jwt.getClaims().get("tenantUserId");
        return v == null ? null : v.toString();
    }

    public static Integer sessionVersion(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object v = jwt.getClaims().get("sessionVersion");
        if (v instanceof Number n) {
            return n.intValue();
        }
        return v == null ? null : Integer.parseInt(v.toString());
    }

    /** Returns "admin" or "tenant" (defaults to "tenant" if absent). */
    public static String type(Jwt jwt) {
        if (jwt == null) {
            return "tenant";
        }
        Object v = jwt.getClaims().get("type");
        return v == null ? "tenant" : v.toString();
    }

    public static String adminUid(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object v = jwt.getClaims().get("adminUid");
        return v == null ? null : v.toString();
    }

    /**
     * Resolves the JWT from the security context. {@code @AuthenticationPrincipal Jwt} can be null
     * even when the request is authenticated; this reads {@link JwtAuthenticationToken} explicitly.
     */
    public static Jwt resolve(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    public static String requireTenantId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        String tenantId = tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = jwt.getSubject();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                "Tenant context is missing from your session. Log in with a tenant account (not platform admin).");
        }
        return tenantId.trim();
    }

    public static String requireTenantId(Authentication authentication) {
        return requireTenantId(resolve(authentication));
    }

    public static boolean isMerchant(Jwt jwt) {
        return jwt != null && "merchant".equals(type(jwt));
    }

    public static String merchantUid(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object claim = jwt.getClaims().get("merchantUid");
        if (claim != null && !claim.toString().isBlank()) {
            return claim.toString().trim();
        }
        return jwt.getSubject();
    }

    public static String requireMerchantUid(Jwt jwt) {
        if (jwt == null || !isMerchant(jwt)) {
            throw new IllegalArgumentException("Merchant authentication required");
        }
        String uid = merchantUid(jwt);
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("Merchant context is missing from your session");
        }
        return uid.trim();
    }

    public static String requireTenantAdmin(Jwt jwt) {
        if (jwt == null || isMerchant(jwt)) {
            throw new IllegalArgumentException("Tenant admin authentication required");
        }
        return requireTenantId(jwt);
    }
}
