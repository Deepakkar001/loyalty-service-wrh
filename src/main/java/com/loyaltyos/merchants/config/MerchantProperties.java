package com.loyaltyos.merchants.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loyaltyos.merchant")
public class MerchantProperties {

    private boolean enabled = true;
    private int maxLoginAttempts = 5;
    private int lockoutMinutes = 30;
    private String defaultPortalPasswordPrefix = "Merchant";
    private int inviteTokenTtlDays = 7;
    private BigDecimal minEarnRateMultiplier = new BigDecimal("0.5");
    private BigDecimal maxEarnRateMultiplier = new BigDecimal("10.0");
    private BigDecimal defaultBudgetAlertPct = new BigDecimal("80");
    private BigDecimal commissionMaxRate = new BigDecimal("10");
    private BigDecimal maxMerchantCampaignBudget = new BigDecimal("500000");

    private boolean financeAgreementGateEnabled = false;
    private boolean makerCheckerConfigEnabled = false;
    private boolean settlementBatchJobEnabled = false;
    private String settlementBatchCron = "0 30 2 1 * *";

    public boolean isSettlementBatchJobEnabled() { return settlementBatchJobEnabled; }
    public void setSettlementBatchJobEnabled(boolean settlementBatchJobEnabled) {
        this.settlementBatchJobEnabled = settlementBatchJobEnabled;
    }

    public String getSettlementBatchCron() { return settlementBatchCron; }
    public void setSettlementBatchCron(String settlementBatchCron) {
        this.settlementBatchCron = settlementBatchCron;
    }

    public boolean isFinanceAgreementGateEnabled() { return financeAgreementGateEnabled; }
    public void setFinanceAgreementGateEnabled(boolean financeAgreementGateEnabled) {
        this.financeAgreementGateEnabled = financeAgreementGateEnabled;
    }

    public boolean isMakerCheckerConfigEnabled() { return makerCheckerConfigEnabled; }
    public void setMakerCheckerConfigEnabled(boolean makerCheckerConfigEnabled) {
        this.makerCheckerConfigEnabled = makerCheckerConfigEnabled;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }

    public int getLockoutMinutes() { return lockoutMinutes; }
    public void setLockoutMinutes(int lockoutMinutes) { this.lockoutMinutes = lockoutMinutes; }

    public String getDefaultPortalPasswordPrefix() { return defaultPortalPasswordPrefix; }
    public void setDefaultPortalPasswordPrefix(String defaultPortalPasswordPrefix) {
        this.defaultPortalPasswordPrefix = defaultPortalPasswordPrefix;
    }

    public int getInviteTokenTtlDays() { return inviteTokenTtlDays; }
    public void setInviteTokenTtlDays(int inviteTokenTtlDays) { this.inviteTokenTtlDays = inviteTokenTtlDays; }

    public BigDecimal getMinEarnRateMultiplier() { return minEarnRateMultiplier; }
    public void setMinEarnRateMultiplier(BigDecimal minEarnRateMultiplier) {
        this.minEarnRateMultiplier = minEarnRateMultiplier;
    }

    public BigDecimal getMaxEarnRateMultiplier() { return maxEarnRateMultiplier; }
    public void setMaxEarnRateMultiplier(BigDecimal maxEarnRateMultiplier) {
        this.maxEarnRateMultiplier = maxEarnRateMultiplier;
    }

    public BigDecimal getDefaultBudgetAlertPct() { return defaultBudgetAlertPct; }
    public void setDefaultBudgetAlertPct(BigDecimal defaultBudgetAlertPct) {
        this.defaultBudgetAlertPct = defaultBudgetAlertPct;
    }

    public BigDecimal getCommissionMaxRate() { return commissionMaxRate; }
    public void setCommissionMaxRate(BigDecimal commissionMaxRate) {
        this.commissionMaxRate = commissionMaxRate;
    }

    public BigDecimal getMaxMerchantCampaignBudget() { return maxMerchantCampaignBudget; }
    public void setMaxMerchantCampaignBudget(BigDecimal maxMerchantCampaignBudget) {
        this.maxMerchantCampaignBudget = maxMerchantCampaignBudget;
    }
}
