package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "programme_config")
public class ProgrammeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "config_version", nullable = false)
    private Integer configVersion;

    @Column(name = "config_json", nullable = false, columnDefinition = "JSON")
    private String configJson;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "created_by_actor_id", length = 64)
    private String createdByActorId;

    @Column(name = "created_by_role", length = 32)
    private String createdByRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public ProgrammeConfig() {}

    public ProgrammeConfig(
        Long id,
        String tenantId,
        String programmeUid,
        Integer configVersion,
        String configJson,
        Instant effectiveFrom,
        String createdByActorId,
        String createdByRole,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.configVersion = configVersion;
        this.configJson = configJson;
        this.effectiveFrom = effectiveFrom;
        this.createdByActorId = createdByActorId;
        this.createdByRole = createdByRole;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid;
        private Integer configVersion;
        private String configJson;
        private Instant effectiveFrom;
        private String createdByActorId;
        private String createdByRole;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder programmeUid(String programmeUid) {
            this.programmeUid = programmeUid;
            return this;
        }

        public Builder configVersion(Integer configVersion) {
            this.configVersion = configVersion;
            return this;
        }

        public Builder configJson(String configJson) {
            this.configJson = configJson;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder createdByActorId(String createdByActorId) {
            this.createdByActorId = createdByActorId;
            return this;
        }

        public Builder createdByRole(String createdByRole) {
            this.createdByRole = createdByRole;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProgrammeConfig build() {
            return new ProgrammeConfig(
                id,
                tenantId,
                programmeUid,
                configVersion,
                configJson,
                effectiveFrom,
                createdByActorId,
                createdByRole,
                createdAt
            );
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public Integer getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Integer configVersion) {
        this.configVersion = configVersion;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getCreatedByActorId() {
        return createdByActorId;
    }

    public void setCreatedByActorId(String createdByActorId) {
        this.createdByActorId = createdByActorId;
    }

    public String getCreatedByRole() {
        return createdByRole;
    }

    public void setCreatedByRole(String createdByRole) {
        this.createdByRole = createdByRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

