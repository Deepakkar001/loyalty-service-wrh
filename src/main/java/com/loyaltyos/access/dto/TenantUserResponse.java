package com.loyaltyos.access.dto;

import java.util.List;

public class TenantUserResponse {
    private String userId;
    private String email;
    private String fullName;
    private String status;
    private boolean mustChangePassword;
    private List<String> roleIds;
    private String inviteToken;
    /** True when SMTP accepted the invite email; false when skipped or temp-password path. */
    private Boolean inviteEmailSent;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public List<String> getRoleIds() { return roleIds; }
    public void setRoleIds(List<String> roleIds) { this.roleIds = roleIds; }
    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
    public Boolean getInviteEmailSent() { return inviteEmailSent; }
    public void setInviteEmailSent(Boolean inviteEmailSent) { this.inviteEmailSent = inviteEmailSent; }
}
