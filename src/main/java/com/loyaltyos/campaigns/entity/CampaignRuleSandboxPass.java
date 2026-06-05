package com.loyaltyos.campaigns.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "campaign_rule_sandbox_pass",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tenant_rule_sandbox_pass",
        columnNames = {"tenant_id", "rule_uid"}
    ),
    indexes = {
        @Index(name = "idx_sandbox_pass_campaign", columnList = "tenant_id,campaign_uid")
    }
)
public class CampaignRuleSandboxPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "campaign_uid", nullable = false, length = 128)
    private String campaignUid;

    @Column(name = "rule_uid", nullable = false, length = 128)
    private String ruleUid;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "targeted_check_ok", nullable = false)
    private boolean targetedCheckOk;

    @Column(name = "passed_by", length = 255)
    private String passedBy;

    @Column(name = "passed_at", nullable = false)
    private Instant passedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public CampaignRuleSandboxPass() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getRuleUid() { return ruleUid; }
    public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public boolean isTargetedCheckOk() { return targetedCheckOk; }
    public void setTargetedCheckOk(boolean targetedCheckOk) { this.targetedCheckOk = targetedCheckOk; }
    public String getPassedBy() { return passedBy; }
    public void setPassedBy(String passedBy) { this.passedBy = passedBy; }
    public Instant getPassedAt() { return passedAt; }
    public void setPassedAt(Instant passedAt) { this.passedAt = passedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
