package com.loyaltyos.merchants.dto;

public class MerchantInviteValidateResponse {

    private boolean valid;
    private String merchantName;
    private String emailMasked;
    private String expiresAt;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getEmailMasked() { return emailMasked; }
    public void setEmailMasked(String emailMasked) { this.emailMasked = emailMasked; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
