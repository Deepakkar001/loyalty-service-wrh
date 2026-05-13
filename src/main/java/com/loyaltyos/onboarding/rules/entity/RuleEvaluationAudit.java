package com.loyaltyos.onboarding.rules.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "rule_evaluation_audit")
public class RuleEvaluationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trace_json", nullable = false, columnDefinition = "JSON")
    private String traceJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public RuleEvaluationAudit() {}

    public RuleEvaluationAudit(
        Long id,
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        boolean success,
        String traceJson,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = (programmeUid == null || programmeUid.isBlank()) ? "default" : programmeUid;
        this.customerId = customerId;
        this.eventId = eventId;
        this.success = success;
        this.traceJson = traceJson;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid = "default";
        private String customerId;
        private String eventId;
        private boolean success;
        private String traceJson;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder traceJson(String traceJson) { this.traceJson = traceJson; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public RuleEvaluationAudit build() {
            return new RuleEvaluationAudit(id, tenantId, programmeUid, customerId, eventId, success, traceJson, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getTraceJson() { return traceJson; }
    public void setTraceJson(String traceJson) { this.traceJson = traceJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
