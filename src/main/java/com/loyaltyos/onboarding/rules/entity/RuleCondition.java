package com.loyaltyos.onboarding.rules.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "rule_conditions")
public class RuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false, unique = true)
    private EarnRule rule;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_tree", nullable = false, columnDefinition = "JSON")
    private JsonNode conditionTree;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** JPA requires a no-arg constructor. */
    public RuleCondition() {}

    public RuleCondition(
        Long id,
        EarnRule rule,
        String tenantId,
        JsonNode conditionTree,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.rule = rule;
        this.tenantId = tenantId;
        this.conditionTree = conditionTree;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private EarnRule rule;
        private String tenantId;
        private JsonNode conditionTree;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder rule(EarnRule rule) { this.rule = rule; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder conditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public RuleCondition build() {
            return new RuleCondition(id, rule, tenantId, conditionTree, createdAt, updatedAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EarnRule getRule() { return rule; }
    public void setRule(EarnRule rule) { this.rule = rule; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public JsonNode getConditionTree() { return conditionTree; }
    public void setConditionTree(JsonNode conditionTree) { this.conditionTree = conditionTree; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
