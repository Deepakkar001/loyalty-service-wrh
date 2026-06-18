package com.loyaltyos.merchants.dto;

public class MerchantInviteLinkResponse {

    private String inviteUrl;
    private String inviteExpiresAt;
    private String portalUsername;

    public String getInviteUrl() { return inviteUrl; }
    public void setInviteUrl(String inviteUrl) { this.inviteUrl = inviteUrl; }

    public String getInviteExpiresAt() { return inviteExpiresAt; }
    public void setInviteExpiresAt(String inviteExpiresAt) { this.inviteExpiresAt = inviteExpiresAt; }

    public String getPortalUsername() { return portalUsername; }
    public void setPortalUsername(String portalUsername) { this.portalUsername = portalUsername; }
}
