package com.loyaltyos.merchants.dto;

public class MerchantResendInviteResponse {

    private boolean inviteEmailSent;
    private String portalUsername;
    private boolean alreadyActivated;
    private String inviteUrl;
    private String inviteExpiresAt;

    public boolean isInviteEmailSent() { return inviteEmailSent; }
    public void setInviteEmailSent(boolean inviteEmailSent) { this.inviteEmailSent = inviteEmailSent; }

    public String getPortalUsername() { return portalUsername; }
    public void setPortalUsername(String portalUsername) { this.portalUsername = portalUsername; }

    public boolean isAlreadyActivated() { return alreadyActivated; }
    public void setAlreadyActivated(boolean alreadyActivated) { this.alreadyActivated = alreadyActivated; }

    public String getInviteUrl() { return inviteUrl; }
    public void setInviteUrl(String inviteUrl) { this.inviteUrl = inviteUrl; }

    public String getInviteExpiresAt() { return inviteExpiresAt; }
    public void setInviteExpiresAt(String inviteExpiresAt) { this.inviteExpiresAt = inviteExpiresAt; }
}
