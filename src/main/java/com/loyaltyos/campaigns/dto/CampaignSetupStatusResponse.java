package com.loyaltyos.campaigns.dto;

import com.loyaltyos.campaigns.enums.CampaignExecutionMode;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import java.time.Instant;

public class CampaignSetupStatusResponse {

    private String campaignUid;
    private CampaignStatus campaignStatus;
    private CampaignExecutionMode executionMode;
    private boolean campaignSaved;
    private boolean campaignRuleCreated;
    private String campaignRuleUid;
    private boolean campaignRuleActive;
    private boolean sandboxPassed;
    private boolean canActivateCampaign;
    private String activateBlockReason;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public CampaignStatus getCampaignStatus() { return campaignStatus; }
    public void setCampaignStatus(CampaignStatus campaignStatus) { this.campaignStatus = campaignStatus; }
    public CampaignExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(CampaignExecutionMode executionMode) { this.executionMode = executionMode; }
    public boolean isCampaignSaved() { return campaignSaved; }
    public void setCampaignSaved(boolean campaignSaved) { this.campaignSaved = campaignSaved; }
    public boolean isCampaignRuleCreated() { return campaignRuleCreated; }
    public void setCampaignRuleCreated(boolean campaignRuleCreated) { this.campaignRuleCreated = campaignRuleCreated; }
    public String getCampaignRuleUid() { return campaignRuleUid; }
    public void setCampaignRuleUid(String campaignRuleUid) { this.campaignRuleUid = campaignRuleUid; }
    public boolean isCampaignRuleActive() { return campaignRuleActive; }
    public void setCampaignRuleActive(boolean campaignRuleActive) { this.campaignRuleActive = campaignRuleActive; }
    public boolean isSandboxPassed() { return sandboxPassed; }
    public void setSandboxPassed(boolean sandboxPassed) { this.sandboxPassed = sandboxPassed; }
    public boolean isCanActivateCampaign() { return canActivateCampaign; }
    public void setCanActivateCampaign(boolean canActivateCampaign) { this.canActivateCampaign = canActivateCampaign; }
    public String getActivateBlockReason() { return activateBlockReason; }
    public void setActivateBlockReason(String activateBlockReason) { this.activateBlockReason = activateBlockReason; }
    private Instant sandboxPassedAt;

    public Instant getSandboxPassedAt() { return sandboxPassedAt; }
    public void setSandboxPassedAt(Instant sandboxPassedAt) { this.sandboxPassedAt = sandboxPassedAt; }
}
