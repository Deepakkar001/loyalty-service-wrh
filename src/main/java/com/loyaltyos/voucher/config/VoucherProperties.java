package com.loyaltyos.voucher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loyaltyos.voucher")
public class VoucherProperties {

    private boolean enabled = true;
    /**
     * When enabled, rules with ISSUE_VOUCHER action can auto-issue a voucher code during event processing
     * (behind action config issueMode=AUTO_ISSUE_ON_EVENT).
     */
    private AutoIssueFromRules autoIssueFromRules = new AutoIssueFromRules();
    private int maxFileSizeMb = 50;
    private int maxRows = 100_000;
    private long lowStockThreshold = 100;
    private boolean expiryJobEnabled = true;
    private String expiryCron = "0 0 2 * * *";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public AutoIssueFromRules getAutoIssueFromRules() { return autoIssueFromRules; }
    public void setAutoIssueFromRules(AutoIssueFromRules autoIssueFromRules) {
        this.autoIssueFromRules = autoIssueFromRules != null ? autoIssueFromRules : new AutoIssueFromRules();
    }
    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
    public long getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(long lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public boolean isExpiryJobEnabled() { return expiryJobEnabled; }
    public void setExpiryJobEnabled(boolean expiryJobEnabled) { this.expiryJobEnabled = expiryJobEnabled; }
    public String getExpiryCron() { return expiryCron; }
    public void setExpiryCron(String expiryCron) { this.expiryCron = expiryCron; }

    public static class AutoIssueFromRules {
        /** Property: loyaltyos.voucher.autoIssueFromRules.enabled (default false). */
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
