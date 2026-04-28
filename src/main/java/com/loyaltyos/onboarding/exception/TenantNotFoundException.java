package com.loyaltyos.onboarding.exception;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(UUID tenantId) {
        super("Tenant not found with id: " + tenantId);
    }
    public TenantNotFoundException(String message) {
        super(message);
    }
}

