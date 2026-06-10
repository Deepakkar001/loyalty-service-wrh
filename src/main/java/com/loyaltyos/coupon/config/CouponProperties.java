package com.loyaltyos.coupon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loyaltyos.coupon")
public class CouponProperties {

    private boolean enabled = true;
    private boolean expiryJobEnabled = true;
    private String expiryCron = "0 15 2 * * *";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isExpiryJobEnabled() { return expiryJobEnabled; }
    public void setExpiryJobEnabled(boolean expiryJobEnabled) { this.expiryJobEnabled = expiryJobEnabled; }
    public String getExpiryCron() { return expiryCron; }
    public void setExpiryCron(String expiryCron) { this.expiryCron = expiryCron; }
}
