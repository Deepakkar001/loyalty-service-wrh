package com.loyaltyos.integration.security;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiKeyAuthenticationTest {

    @Test
    void getNameReturnsTenantIdWithoutRecursion() {
        ApiKeyAuthentication auth = new ApiKeyAuthentication(
            "tenant-abc", "key-uid-1", ApiKeyEnvironment.SANDBOX);

        assertEquals("tenant-abc", auth.getName());
        assertEquals("tenant-abc", auth.getTenantId());
        ApiKeyPrincipal principal = (ApiKeyPrincipal) auth.getPrincipal();
        assertEquals("tenant-abc", principal.tenantId());
        assertEquals("key-uid-1", principal.keyUid());
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                auth.getName();
            }
        });
    }
}
