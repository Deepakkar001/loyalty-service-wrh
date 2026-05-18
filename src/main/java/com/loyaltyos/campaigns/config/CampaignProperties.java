package com.loyaltyos.campaigns.config;



import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "loyalty.campaigns")

public class CampaignProperties {



    private boolean enabled = true;

    private BigDecimal approvalBudgetThreshold = new BigDecimal("100000");

    private BigDecimal defaultAlertThresholdPct = new BigDecimal("80");

    private boolean resolutionLogEnabled = true;

    private boolean budgetAlertWebhookEnabled = true;

    private String budgetAlertKafkaTopic = "platform.campaign.alerts";

    private boolean exhaustedJobEnabled = true;

    private long exhaustedFixedDelayMs = 300_000L;



    public boolean isEnabled() {

        return enabled;

    }



    public void setEnabled(boolean enabled) {

        this.enabled = enabled;

    }



    public BigDecimal getApprovalBudgetThreshold() {

        return approvalBudgetThreshold;

    }



    public void setApprovalBudgetThreshold(BigDecimal approvalBudgetThreshold) {

        this.approvalBudgetThreshold = approvalBudgetThreshold;

    }



    public BigDecimal getDefaultAlertThresholdPct() {

        return defaultAlertThresholdPct;

    }



    public void setDefaultAlertThresholdPct(BigDecimal defaultAlertThresholdPct) {

        this.defaultAlertThresholdPct = defaultAlertThresholdPct;

    }



    public boolean isResolutionLogEnabled() {

        return resolutionLogEnabled;

    }



    public void setResolutionLogEnabled(boolean resolutionLogEnabled) {

        this.resolutionLogEnabled = resolutionLogEnabled;

    }



    public boolean isBudgetAlertWebhookEnabled() {

        return budgetAlertWebhookEnabled;

    }



    public void setBudgetAlertWebhookEnabled(boolean budgetAlertWebhookEnabled) {

        this.budgetAlertWebhookEnabled = budgetAlertWebhookEnabled;

    }



    public String getBudgetAlertKafkaTopic() {

        return budgetAlertKafkaTopic;

    }



    public void setBudgetAlertKafkaTopic(String budgetAlertKafkaTopic) {

        this.budgetAlertKafkaTopic = budgetAlertKafkaTopic;

    }



    public boolean isExhaustedJobEnabled() {

        return exhaustedJobEnabled;

    }



    public void setExhaustedJobEnabled(boolean exhaustedJobEnabled) {

        this.exhaustedJobEnabled = exhaustedJobEnabled;

    }



    public long getExhaustedFixedDelayMs() {

        return exhaustedFixedDelayMs;

    }



    public void setExhaustedFixedDelayMs(long exhaustedFixedDelayMs) {

        this.exhaustedFixedDelayMs = exhaustedFixedDelayMs;

    }

}

