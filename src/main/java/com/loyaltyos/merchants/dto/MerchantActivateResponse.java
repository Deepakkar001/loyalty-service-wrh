package com.loyaltyos.merchants.dto;

public class MerchantActivateResponse extends MerchantResponse {

    private String portalUsername;
    private boolean inviteEmailSent;
    private String inviteUrl;
    private String inviteExpiresAt;

    public String getPortalUsername() { return portalUsername; }
    public void setPortalUsername(String portalUsername) { this.portalUsername = portalUsername; }

    public boolean isInviteEmailSent() { return inviteEmailSent; }
    public void setInviteEmailSent(boolean inviteEmailSent) { this.inviteEmailSent = inviteEmailSent; }

    public String getInviteUrl() { return inviteUrl; }
    public void setInviteUrl(String inviteUrl) { this.inviteUrl = inviteUrl; }

    public String getInviteExpiresAt() { return inviteExpiresAt; }
    public void setInviteExpiresAt(String inviteExpiresAt) { this.inviteExpiresAt = inviteExpiresAt; }
}
