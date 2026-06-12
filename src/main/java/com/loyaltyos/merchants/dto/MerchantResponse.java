package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class MerchantResponse {

    private String merchantUid;
    private String legalName;
    private String displayName;
    private String category;
    private String contactEmail;
    private String onboardingStage;
    private BigDecimal earnRateMultiplier;
    private String settlementCycle;
    private Instant agreementAcceptedAt;
    private Instant integrationTestPassedAt;
    private boolean active;
    private boolean suspended;
    private Instant createdAt;

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getOnboardingStage() { return onboardingStage; }
    public void setOnboardingStage(String onboardingStage) { this.onboardingStage = onboardingStage; }

    public BigDecimal getEarnRateMultiplier() { return earnRateMultiplier; }
    public void setEarnRateMultiplier(BigDecimal earnRateMultiplier) { this.earnRateMultiplier = earnRateMultiplier; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }

    public Instant getAgreementAcceptedAt() { return agreementAcceptedAt; }
    public void setAgreementAcceptedAt(Instant agreementAcceptedAt) { this.agreementAcceptedAt = agreementAcceptedAt; }

    public Instant getIntegrationTestPassedAt() { return integrationTestPassedAt; }
    public void setIntegrationTestPassedAt(Instant integrationTestPassedAt) {
        this.integrationTestPassedAt = integrationTestPassedAt;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
