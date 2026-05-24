package com.loyaltyos.integration.security;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;

/**
 * Principal for API-key authenticated integration requests.
 * Used with {@code @AuthenticationPrincipal(ApiKeyPrincipal.class)}.
 */
public record ApiKeyPrincipal(String tenantId, String keyUid, ApiKeyEnvironment environment) {
}
