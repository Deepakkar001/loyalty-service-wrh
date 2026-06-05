package com.loyaltyos.campaigns.config;



import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "loyalty.campaigns")

public class CampaignProperties {

    /**
     * Controls how {@code /events/process} applies campaigns vs programme rules.
     */
    public enum EventProcessingMode {
        /** Backward-compatible: evaluate both campaigns (if enabled) and programme rules for every event. */
        LEGACY_BOTH,
        /**
         * Mutually exclusive: if request metadata includes {@code evaluationScope=CAMPAIGN} then evaluate campaigns only;
         * otherwise evaluate programme rules only.
         */
        SEPARATE_BY_METADATA
    }



    private boolean enabled = true;

    private BigDecimal approvalBudgetThreshold = new BigDecimal("100000");

    private BigDecimal defaultAlertThresholdPct = new BigDecimal("80");

    private boolean resolutionLogEnabled = true;

    private EventProcessingMode eventProcessingMode = EventProcessingMode.LEGACY_BOTH;

    private boolean budgetAlertWebhookEnabled = true;

    private String budgetAlertKafkaTopic = "platform.campaign.alerts";

    private boolean exhaustedJobEnabled = true;

    private long exhaustedFixedDelayMs = 300_000L;

    private boolean expiredJobEnabled = true;

    private long expiredFixedDelayMs = 300_000L;

    private TargetCustomerUpload targetCustomerUpload = new TargetCustomerUpload();

    /** When true, RULE_GATED campaigns require sandbox pass + ACTIVE CAMPAIGN rule before activation. */
    private boolean ruleGatedOnly = true;

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

    public EventProcessingMode getEventProcessingMode() {
        return eventProcessingMode;
    }

    public void setEventProcessingMode(EventProcessingMode eventProcessingMode) {
        this.eventProcessingMode = eventProcessingMode;
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

    public boolean isExpiredJobEnabled() {
        return expiredJobEnabled;
    }

    public void setExpiredJobEnabled(boolean expiredJobEnabled) {
        this.expiredJobEnabled = expiredJobEnabled;
    }

    public long getExpiredFixedDelayMs() {
        return expiredFixedDelayMs;
    }

    public void setExpiredFixedDelayMs(long expiredFixedDelayMs) {
        this.expiredFixedDelayMs = expiredFixedDelayMs;
    }

    public TargetCustomerUpload getTargetCustomerUpload() {
        return targetCustomerUpload != null ? targetCustomerUpload : new TargetCustomerUpload();
    }

    public void setTargetCustomerUpload(TargetCustomerUpload targetCustomerUpload) {
        this.targetCustomerUpload = targetCustomerUpload != null ? targetCustomerUpload : new TargetCustomerUpload();
    }

    public boolean isRuleGatedOnly() {
        return ruleGatedOnly;
    }

    public void setRuleGatedOnly(boolean ruleGatedOnly) {
        this.ruleGatedOnly = ruleGatedOnly;
    }

    public static class TargetCustomerUpload {
        private int maxFileSizeMb = 10;
        private int maxRows = 50_000;
        private int maxCustomerIdLength = 128;
        private int minCustomerIdLength = 1;

        public int getMaxFileSizeMb() { return maxFileSizeMb; }
        public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
        public int getMaxRows() { return maxRows; }
        public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
        public int getMaxCustomerIdLength() { return maxCustomerIdLength; }
        public void setMaxCustomerIdLength(int maxCustomerIdLength) { this.maxCustomerIdLength = maxCustomerIdLength; }
        public int getMinCustomerIdLength() { return minCustomerIdLength; }
        public void setMinCustomerIdLength(int minCustomerIdLength) { this.minCustomerIdLength = minCustomerIdLength; }
    }

}

