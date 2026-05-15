package com.loyaltyos.onboarding.rewards.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loyalty.rewards")
public class RewardEngineProperties {

    /** When true, append SUCCESS rows to reward_issuance_audit in the same DB transaction as ledger inserts. */
    private boolean issuanceAuditEnabled = true;

    /** When true, persist FAILED rows in a separate transaction (survives rollback of issuance). */
    private boolean failureAuditEnabled = true;

    /** When true, scheduled expiry job is registered. */
    private boolean expiryJobEnabled = false;

    /** Max CREDIT rows processed per expiry scheduler tick. */
    private int expiryBatchLimit = 500;

    /** When true, scheduled reconciliation job is registered. */
    private boolean reconciliationJobEnabled = false;

    /** When true, reconciliation sets cache = ledger sum if variance != 0. Default false (log only). */
    private boolean reconciliationAutoFixEnabled = false;

    /** Max customers scanned per tenant per reconciliation tick. */
    private int reconciliationCustomerBatchLimit = 200;

    /**
     * Months from issuance {@code Instant} until {@code expires_at} on new CREDIT rows.
     * When {@code <= 0}, credits do not expire via time (column left null).
     */
    private int defaultCreditExpiryMonths = 24;

    /**
     * When true, a failure to persist SUCCESS {@code reward_issuance_audit} rolls back the issuance transaction.
     * Default false so a broken audit path cannot block earning in production.
     */
    private boolean issuanceAuditStrict = false;

    public boolean isIssuanceAuditEnabled() {
        return issuanceAuditEnabled;
    }

    public void setIssuanceAuditEnabled(boolean issuanceAuditEnabled) {
        this.issuanceAuditEnabled = issuanceAuditEnabled;
    }

    public boolean isFailureAuditEnabled() {
        return failureAuditEnabled;
    }

    public void setFailureAuditEnabled(boolean failureAuditEnabled) {
        this.failureAuditEnabled = failureAuditEnabled;
    }

    public boolean isExpiryJobEnabled() {
        return expiryJobEnabled;
    }

    public void setExpiryJobEnabled(boolean expiryJobEnabled) {
        this.expiryJobEnabled = expiryJobEnabled;
    }

    public int getExpiryBatchLimit() {
        return expiryBatchLimit;
    }

    public void setExpiryBatchLimit(int expiryBatchLimit) {
        this.expiryBatchLimit = expiryBatchLimit;
    }

    public boolean isReconciliationJobEnabled() {
        return reconciliationJobEnabled;
    }

    public void setReconciliationJobEnabled(boolean reconciliationJobEnabled) {
        this.reconciliationJobEnabled = reconciliationJobEnabled;
    }

    public boolean isReconciliationAutoFixEnabled() {
        return reconciliationAutoFixEnabled;
    }

    public void setReconciliationAutoFixEnabled(boolean reconciliationAutoFixEnabled) {
        this.reconciliationAutoFixEnabled = reconciliationAutoFixEnabled;
    }

    public int getReconciliationCustomerBatchLimit() {
        return reconciliationCustomerBatchLimit;
    }

    public void setReconciliationCustomerBatchLimit(int reconciliationCustomerBatchLimit) {
        this.reconciliationCustomerBatchLimit = reconciliationCustomerBatchLimit;
    }

    public int getDefaultCreditExpiryMonths() {
        return defaultCreditExpiryMonths;
    }

    public void setDefaultCreditExpiryMonths(int defaultCreditExpiryMonths) {
        this.defaultCreditExpiryMonths = defaultCreditExpiryMonths;
    }

    public boolean isIssuanceAuditStrict() {
        return issuanceAuditStrict;
    }

    public void setIssuanceAuditStrict(boolean issuanceAuditStrict) {
        this.issuanceAuditStrict = issuanceAuditStrict;
    }
}
