package com.loyaltyos.referrals.entity;

import com.loyaltyos.referrals.enums.ReferralStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "referrals")
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid = "default";

    @Column(name = "referral_uid", nullable = false, length = 128)
    private String referralUid;

    @Column(name = "referrer_customer_id", nullable = false, length = 128)
    private String referrerCustomerId;

    @Column(name = "referee_customer_id", length = 128)
    private String refereeCustomerId;

    @Column(name = "referral_code_used", length = 64)
    private String referralCodeUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "current_stage", nullable = false)
    private int currentStage = 0;

    @Column(name = "completed_stages_json", columnDefinition = "json")
    private String completedStagesJson;

    @Column(name = "progress_json", columnDefinition = "json")
    private String progressJson;

    @Column(name = "purchase_count", nullable = false)
    private int purchaseCount = 0;

    @Column(name = "total_spend", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalSpend = BigDecimal.ZERO;

    @Column(name = "fraud_result_json", columnDefinition = "json")
    private String fraudResultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getReferralUid() { return referralUid; }
    public void setReferralUid(String referralUid) { this.referralUid = referralUid; }
    public String getReferrerCustomerId() { return referrerCustomerId; }
    public void setReferrerCustomerId(String referrerCustomerId) { this.referrerCustomerId = referrerCustomerId; }
    public String getRefereeCustomerId() { return refereeCustomerId; }
    public void setRefereeCustomerId(String refereeCustomerId) { this.refereeCustomerId = refereeCustomerId; }
    public String getReferralCodeUsed() { return referralCodeUsed; }
    public void setReferralCodeUsed(String referralCodeUsed) { this.referralCodeUsed = referralCodeUsed; }
    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus status) { this.status = status; }
    public int getCurrentStage() { return currentStage; }
    public void setCurrentStage(int currentStage) { this.currentStage = currentStage; }
    public String getCompletedStagesJson() { return completedStagesJson; }
    public void setCompletedStagesJson(String completedStagesJson) { this.completedStagesJson = completedStagesJson; }
    public String getProgressJson() { return progressJson; }
    public void setProgressJson(String progressJson) { this.progressJson = progressJson; }
    public int getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(int purchaseCount) { this.purchaseCount = purchaseCount; }
    public BigDecimal getTotalSpend() { return totalSpend; }
    public void setTotalSpend(BigDecimal totalSpend) { this.totalSpend = totalSpend; }
    public String getFraudResultJson() { return fraudResultJson; }
    public void setFraudResultJson(String fraudResultJson) { this.fraudResultJson = fraudResultJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

