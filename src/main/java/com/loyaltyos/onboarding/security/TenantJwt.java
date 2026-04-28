package com.loyaltyos.onboarding.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class TenantJwt {
    private TenantJwt() {}

    public static String tenantId(Jwt jwt) {
        Object v = jwt.getClaims().get("tenantId");
        return v == null ? null : v.toString();
    }

    public static String email(Jwt jwt) {
        Object v = jwt.getClaims().get("email");
        return v == null ? null : v.toString();
    }

    public static String role(Jwt jwt) {
        Object v = jwt.getClaims().get("role");
        return v == null ? null : v.toString();
    }

    /** Returns "admin" or "tenant" (defaults to "tenant" if absent). */
    public static String type(Jwt jwt) {
        Object v = jwt.getClaims().get("type");
        return v == null ? "tenant" : v.toString();
    }

    public static String adminUid(Jwt jwt) {
        Object v = jwt.getClaims().get("adminUid");
        return v == null ? null : v.toString();
    }
}

