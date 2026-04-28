package com.loyaltyos.onboarding.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Published to Kafka topic: platform.config.updates
 * When a tenant's configuration changes AFTER activation.
 */
@Getter
@Builder
public class TenantConfigUpdatedEvent {

    private final String schemaVersion = "1.0";
    private String tenantId;
    private String changeType;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private Instant changedAt;
    private String changedByAdminId;
}

