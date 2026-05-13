package com.loyaltyos.onboarding.event;

import java.time.Instant;
import java.util.List;

/**
 * Published to Kafka topic: platform.config.updates
 * When a programme's canonical configuration changes (runtime hot-reload signal).
 */
public class ProgrammeConfigUpdatedEvent {

    private final String schemaVersion = "1.0";

    private String tenantId;
    private String programmeId; // "default" for legacy Step 4
    private Integer configVersion;
    private List<String> changedSections;
    private Instant changedAt;
    private String changedByActorId;
    private String changedByActorRole;

    public ProgrammeConfigUpdatedEvent() {}

    public ProgrammeConfigUpdatedEvent(
        String tenantId,
        String programmeId,
        Integer configVersion,
        List<String> changedSections,
        Instant changedAt,
        String changedByActorId,
        String changedByActorRole
    ) {
        this.tenantId = tenantId;
        this.programmeId = programmeId;
        this.configVersion = configVersion;
        this.changedSections = changedSections;
        this.changedAt = changedAt;
        this.changedByActorId = changedByActorId;
        this.changedByActorRole = changedByActorRole;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String programmeId;
        private Integer configVersion;
        private List<String> changedSections;
        private Instant changedAt;
        private String changedByActorId;
        private String changedByActorRole;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeId(String programmeId) { this.programmeId = programmeId; return this; }
        public Builder configVersion(Integer configVersion) { this.configVersion = configVersion; return this; }
        public Builder changedSections(List<String> changedSections) { this.changedSections = changedSections; return this; }
        public Builder changedAt(Instant changedAt) { this.changedAt = changedAt; return this; }
        public Builder changedByActorId(String changedByActorId) { this.changedByActorId = changedByActorId; return this; }
        public Builder changedByActorRole(String changedByActorRole) { this.changedByActorRole = changedByActorRole; return this; }

        public ProgrammeConfigUpdatedEvent build() {
            return new ProgrammeConfigUpdatedEvent(
                tenantId,
                programmeId,
                configVersion,
                changedSections,
                changedAt,
                changedByActorId,
                changedByActorRole
            );
        }
    }

    public String getSchemaVersion() { return schemaVersion; }
    public String getTenantId() { return tenantId; }
    public String getProgrammeId() { return programmeId; }
    public Integer getConfigVersion() { return configVersion; }
    public List<String> getChangedSections() { return changedSections; }
    public Instant getChangedAt() { return changedAt; }
    public String getChangedByActorId() { return changedByActorId; }
    public String getChangedByActorRole() { return changedByActorRole; }
}

