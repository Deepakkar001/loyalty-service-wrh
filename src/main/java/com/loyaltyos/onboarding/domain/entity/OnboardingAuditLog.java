package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "onboarding_audit_log")
public class OnboardingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "actor_id", length = 128)
    private String actorId;

    @Column(name = "actor_role", length = 64)
    private String actorRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "JSON")
    private Map<String, Object> beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "JSON")
    private Map<String, Object> afterState;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public OnboardingAuditLog() {}

    public OnboardingAuditLog(
        Long id,
        String tenantId,
        String action,
        String actorId,
        String actorRole,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        String ipAddress,
        String userAgent,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.action = action;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String action;
        private String actorId;
        private String actorRole;
        private Map<String, Object> beforeState;
        private Map<String, Object> afterState;
        private String ipAddress;
        private String userAgent;
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

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorRole(String actorRole) {
            this.actorRole = actorRole;
            return this;
        }

        public Builder beforeState(Map<String, Object> beforeState) {
            this.beforeState = beforeState;
            return this;
        }

        public Builder afterState(Map<String, Object> afterState) {
            this.afterState = afterState;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OnboardingAuditLog build() {
            return new OnboardingAuditLog(
                id,
                tenantId,
                action,
                actorId,
                actorRole,
                beforeState,
                afterState,
                ipAddress,
                userAgent,
                createdAt
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAction() {
        return action;
    }

    public String getActorId() {
        return actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public Map<String, Object> getBeforeState() {
        return beforeState;
    }

    public Map<String, Object> getAfterState() {
        return afterState;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

