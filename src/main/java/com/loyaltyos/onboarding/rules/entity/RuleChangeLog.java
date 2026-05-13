package com.loyaltyos.onboarding.rules.entity;

import com.loyaltyos.onboarding.rules.enums.RuleChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "rule_change_log")
public class RuleChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private EarnRule rule;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 32)
    private RuleChangeType changeType;

    @Column(name = "changed_by", nullable = false, length = 255)
    private String changedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "JSON")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "JSON")
    private String afterState;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    /** JPA requires a no-arg constructor. */
    public RuleChangeLog() {}

    public RuleChangeLog(
        Long id,
        EarnRule rule,
        String tenantId,
        RuleChangeType changeType,
        String changedBy,
        String beforeState,
        String afterState,
        Instant changedAt
    ) {
        this.id = id;
        this.rule = rule;
        this.tenantId = tenantId;
        this.changeType = changeType;
        this.changedBy = changedBy;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.changedAt = changedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private EarnRule rule;
        private String tenantId;
        private RuleChangeType changeType;
        private String changedBy;
        private String beforeState;
        private String afterState;
        private Instant changedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder rule(EarnRule rule) { this.rule = rule; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder changeType(RuleChangeType changeType) { this.changeType = changeType; return this; }
        public Builder changedBy(String changedBy) { this.changedBy = changedBy; return this; }
        public Builder beforeState(String beforeState) { this.beforeState = beforeState; return this; }
        public Builder afterState(String afterState) { this.afterState = afterState; return this; }
        public Builder changedAt(Instant changedAt) { this.changedAt = changedAt; return this; }

        public RuleChangeLog build() {
            return new RuleChangeLog(id, rule, tenantId, changeType, changedBy, beforeState, afterState, changedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EarnRule getRule() { return rule; }
    public void setRule(EarnRule rule) { this.rule = rule; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public RuleChangeType getChangeType() { return changeType; }
    public void setChangeType(RuleChangeType changeType) { this.changeType = changeType; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }
    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}
