package com.loyaltyos.onboarding.rules.entity;

import com.loyaltyos.onboarding.rules.enums.ExecutionMode;
import com.loyaltyos.onboarding.rules.enums.RuleStatus;
import com.loyaltyos.onboarding.rules.enums.RuleType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "earn_rules",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tenant_programme_rule",
        columnNames = {"tenant_id", "programme_uid", "rule_uid"}
    ),
    indexes = {
        @Index(name = "idx_tenant_prog_status_trigger", columnList = "tenant_id,programme_uid,status,trigger_event_type"),
        @Index(name = "idx_tenant_prog_priority", columnList = "tenant_id,programme_uid,priority")
    }
)
public class EarnRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 32)
    private RuleType ruleType = RuleType.PROGRAMME;

    @Column(name = "campaign_uid", length = 128)
    private String campaignUid;

    @Column(name = "rule_uid", nullable = false, length = 128)
    private String ruleUid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RuleStatus status = RuleStatus.DRAFT;

    @Column(name = "trigger_event_type", nullable = false, length = 64)
    private String triggerEventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 32)
    private ExecutionMode executionMode = ExecutionMode.ALL_MATCHING;

    @Column(name = "effective_at")
    private Instant effectiveAt;

    @Column(name = "end_at")
    private Instant endAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToOne(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RuleCondition condition;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RuleAction> actions = new ArrayList<>();

    /** JPA requires a no-arg constructor. */
    public EarnRule() {}

    public EarnRule(
        Long id,
        String tenantId,
        String programmeUid,
        RuleType ruleType,
        String campaignUid,
        String ruleUid,
        String name,
        String description,
        Integer priority,
        RuleStatus status,
        String triggerEventType,
        ExecutionMode executionMode,
        Instant effectiveAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant archivedAt,
        RuleCondition condition,
        List<RuleAction> actions
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = (programmeUid == null || programmeUid.isBlank()) ? "default" : programmeUid;
        this.ruleType = ruleType != null ? ruleType : RuleType.PROGRAMME;
        this.campaignUid = campaignUid;
        this.ruleUid = ruleUid;
        this.name = name;
        this.description = description;
        this.priority = priority != null ? priority : 0;
        this.status = status != null ? status : RuleStatus.DRAFT;
        this.triggerEventType = triggerEventType;
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.ALL_MATCHING;
        this.effectiveAt = effectiveAt;
        this.endAt = endAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.activatedAt = activatedAt;
        this.archivedAt = archivedAt;
        this.condition = condition;
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid = "default";
        private RuleType ruleType = RuleType.PROGRAMME;
        private String campaignUid;
        private String ruleUid;
        private String name;
        private String description;
        private Integer priority = 0;
        private RuleStatus status = RuleStatus.DRAFT;
        private String triggerEventType;
        private ExecutionMode executionMode = ExecutionMode.ALL_MATCHING;
        private Instant effectiveAt;
        private Instant endAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant activatedAt;
        private Instant archivedAt;
        private RuleCondition condition;
        private List<RuleAction> actions = new ArrayList<>();

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder ruleType(RuleType ruleType) { this.ruleType = ruleType; return this; }
        public Builder campaignUid(String campaignUid) { this.campaignUid = campaignUid; return this; }
        public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder status(RuleStatus status) { this.status = status; return this; }
        public Builder triggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; return this; }
        public Builder executionMode(ExecutionMode executionMode) { this.executionMode = executionMode; return this; }
        public Builder effectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; return this; }
        public Builder endAt(Instant endAt) { this.endAt = endAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder archivedAt(Instant archivedAt) { this.archivedAt = archivedAt; return this; }
        public Builder condition(RuleCondition condition) { this.condition = condition; return this; }
        public Builder actions(List<RuleAction> actions) { this.actions = actions != null ? actions : new ArrayList<>(); return this; }

        public EarnRule build() {
            return new EarnRule(
                id, tenantId, programmeUid, ruleType, campaignUid, ruleUid, name, description, priority, status,
                triggerEventType, executionMode, effectiveAt, endAt, createdAt, updatedAt,
                activatedAt, archivedAt, condition, actions
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public RuleStatus getStatus() { return status; }
    public void setStatus(RuleStatus status) { this.status = status; }
    public String getTriggerEventType() { return triggerEventType; }
    public void setTriggerEventType(String triggerEventType) { this.triggerEventType = triggerEventType; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public RuleCondition getCondition() { return condition; }
    public void setCondition(RuleCondition condition) { this.condition = condition; }
    public List<RuleAction> getActions() { return actions; }
    public void setActions(List<RuleAction> actions) { this.actions = actions != null ? actions : new ArrayList<>(); }
}
