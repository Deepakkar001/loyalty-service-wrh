package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "programmes")
public class Programme {

    public enum ProgrammeStatus { DRAFT, ACTIVE, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProgrammeStatus status = ProgrammeStatus.DRAFT;

    @Column(name = "active_config_version", nullable = false)
    private Integer activeConfigVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** JPA requires a no-arg constructor. */
    public Programme() {}

    public Programme(
        Long id,
        String tenantId,
        String programmeUid,
        String name,
        ProgrammeStatus status,
        Integer activeConfigVersion,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.name = name;
        this.status = status != null ? status : ProgrammeStatus.DRAFT;
        this.activeConfigVersion = activeConfigVersion != null ? activeConfigVersion : 0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid;
        private String name;
        private ProgrammeStatus status = ProgrammeStatus.DRAFT;
        private Integer activeConfigVersion = 0;
        private Instant createdAt;
        private Instant updatedAt;

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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(ProgrammeStatus status) {
            this.status = status;
            return this;
        }

        public Builder activeConfigVersion(Integer activeConfigVersion) {
            this.activeConfigVersion = activeConfigVersion;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Programme build() {
            return new Programme(id, tenantId, programmeUid, name, status, activeConfigVersion, createdAt, updatedAt);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProgrammeStatus getStatus() {
        return status;
    }

    public void setStatus(ProgrammeStatus status) {
        this.status = status;
    }

    public Integer getActiveConfigVersion() {
        return activeConfigVersion;
    }

    public void setActiveConfigVersion(Integer activeConfigVersion) {
        this.activeConfigVersion = activeConfigVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

