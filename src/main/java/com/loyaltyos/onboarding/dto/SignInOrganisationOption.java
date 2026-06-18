package com.loyaltyos.onboarding.dto;

public class SignInOrganisationOption {

    private String tenantId;
    private String merchantUid;
    private String merchantName;

    public SignInOrganisationOption() {}

    public SignInOrganisationOption(String tenantId, String merchantUid, String merchantName) {
        this.tenantId = tenantId;
        this.merchantUid = merchantUid;
        this.merchantName = merchantName;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
}
