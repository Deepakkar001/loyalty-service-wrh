package com.loyaltyos.merchants.dto;

public class MerchantActivateResponse extends MerchantResponse {

    private String portalUsername;
    private String temporaryPassword;

    public String getPortalUsername() { return portalUsername; }
    public void setPortalUsername(String portalUsername) { this.portalUsername = portalUsername; }

    public String getTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }
}
