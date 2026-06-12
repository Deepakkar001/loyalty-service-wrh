package com.loyaltyos.merchants.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loyaltyos.merchant")
public class MerchantProperties {

    private boolean enabled = true;
    private int maxLoginAttempts = 5;
    private int lockoutMinutes = 30;
    private String defaultPortalPasswordPrefix = "Merchant";

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
}
