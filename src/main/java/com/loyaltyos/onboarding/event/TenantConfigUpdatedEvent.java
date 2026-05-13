package com.loyaltyos.onboarding.event;

import java.time.Instant;

/**
 * Published to Kafka topic: platform.config.updates
 * When a tenant's configuration changes AFTER activation.
 */
public class TenantConfigUpdatedEvent {

    private final String schemaVersion = "1.0";
    private String tenantId;
    private String changeType;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private Instant changedAt;
    private String changedByAdminId;

    public TenantConfigUpdatedEvent() {}

    public TenantConfigUpdatedEvent(
        String tenantId,
        String changeType,
        String fieldChanged,
        String oldValue,
        String newValue,
        Instant changedAt,
        String changedByAdminId
    ) {
        this.tenantId = tenantId;
        this.changeType = changeType;
        this.fieldChanged = fieldChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = changedAt;
        this.changedByAdminId = changedByAdminId;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String changeType;
        private String fieldChanged;
        private String oldValue;
        private String newValue;
        private Instant changedAt;
        private String changedByAdminId;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder changeType(String changeType) { this.changeType = changeType; return this; }
        public Builder fieldChanged(String fieldChanged) { this.fieldChanged = fieldChanged; return this; }
        public Builder oldValue(String oldValue) { this.oldValue = oldValue; return this; }
        public Builder newValue(String newValue) { this.newValue = newValue; return this; }
        public Builder changedAt(Instant changedAt) { this.changedAt = changedAt; return this; }
        public Builder changedByAdminId(String changedByAdminId) { this.changedByAdminId = changedByAdminId; return this; }

        public TenantConfigUpdatedEvent build() {
            return new TenantConfigUpdatedEvent(tenantId, changeType, fieldChanged, oldValue, newValue, changedAt, changedByAdminId);
        }
    }

    public String getSchemaVersion() { return schemaVersion; }
    public String getTenantId() { return tenantId; }
    public String getChangeType() { return changeType; }
    public String getFieldChanged() { return fieldChanged; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getChangedAt() { return changedAt; }
    public String getChangedByAdminId() { return changedByAdminId; }
}

