package com.loyaltyos.onboarding.rules.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.onboarding.rules.enums.ActionType;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "rule_actions")
public class RuleAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private EarnRule rule;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "action_uid", nullable = false, length = 128)
    private String actionUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private ActionType actionType;

    @Column(name = "formula", length = 512)
    private String formula;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "JSON")
    private JsonNode config;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** JPA requires a no-arg constructor. */
    public RuleAction() {}

    public RuleAction(
        Long id,
        EarnRule rule,
        String tenantId,
        String actionUid,
        ActionType actionType,
        String formula,
        JsonNode config,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.rule = rule;
        this.tenantId = tenantId;
        this.actionUid = actionUid;
        this.actionType = actionType;
        this.formula = formula;
        this.config = config;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private EarnRule rule;
        private String tenantId;
        private String actionUid;
        private ActionType actionType;
        private String formula;
        private JsonNode config;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder rule(EarnRule rule) { this.rule = rule; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder actionUid(String actionUid) { this.actionUid = actionUid; return this; }
        public Builder actionType(ActionType actionType) { this.actionType = actionType; return this; }
        public Builder formula(String formula) { this.formula = formula; return this; }
        public Builder config(JsonNode config) { this.config = config; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public RuleAction build() {
            return new RuleAction(id, rule, tenantId, actionUid, actionType, formula, config, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EarnRule getRule() { return rule; }
    public void setRule(EarnRule rule) { this.rule = rule; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getActionUid() { return actionUid; }
    public void setActionUid(String actionUid) { this.actionUid = actionUid; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public JsonNode getConfig() { return config; }
    public void setConfig(JsonNode config) { this.config = config; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
