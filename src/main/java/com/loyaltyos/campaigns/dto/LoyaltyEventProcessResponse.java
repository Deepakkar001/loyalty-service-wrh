package com.loyaltyos.campaigns.dto;

import com.loyaltyos.campaigns.model.DroppedCampaign;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LoyaltyEventProcessResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    private String eventId;
    private boolean success;
    private String message;
    private boolean idempotentReplay;
    private BigDecimal rulePointsAwarded = BigDecimal.ZERO;
    private BigDecimal campaignPointsAwarded = BigDecimal.ZERO;
    private BigDecimal totalPointsAwarded = BigDecimal.ZERO;
    private BigDecimal newBalance;
    private boolean programmeCapApplied;
    private String resolutionMode;
    private List<AppliedCampaignLine> campaignsApplied = new ArrayList<>();
    private List<DroppedCampaign> campaignsDropped = new ArrayList<>();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isIdempotentReplay() {
        return idempotentReplay;
    }

    public void setIdempotentReplay(boolean idempotentReplay) {
        this.idempotentReplay = idempotentReplay;
    }

    public BigDecimal getRulePointsAwarded() {
        return rulePointsAwarded;
    }

    public void setRulePointsAwarded(BigDecimal rulePointsAwarded) {
        this.rulePointsAwarded = rulePointsAwarded;
    }

    public BigDecimal getCampaignPointsAwarded() {
        return campaignPointsAwarded;
    }

    public void setCampaignPointsAwarded(BigDecimal campaignPointsAwarded) {
        this.campaignPointsAwarded = campaignPointsAwarded;
    }

    public BigDecimal getTotalPointsAwarded() {
        return totalPointsAwarded;
    }

    public void setTotalPointsAwarded(BigDecimal totalPointsAwarded) {
        this.totalPointsAwarded = totalPointsAwarded;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public boolean isProgrammeCapApplied() {
        return programmeCapApplied;
    }

    public void setProgrammeCapApplied(boolean programmeCapApplied) {
        this.programmeCapApplied = programmeCapApplied;
    }

    public String getResolutionMode() {
        return resolutionMode;
    }

    public void setResolutionMode(String resolutionMode) {
        this.resolutionMode = resolutionMode;
    }

    public List<AppliedCampaignLine> getCampaignsApplied() {
        return campaignsApplied;
    }

    public void setCampaignsApplied(List<AppliedCampaignLine> campaignsApplied) {
        this.campaignsApplied = campaignsApplied;
    }

    public List<DroppedCampaign> getCampaignsDropped() {
        return campaignsDropped;
    }

    public void setCampaignsDropped(List<DroppedCampaign> campaignsDropped) {
        this.campaignsDropped = campaignsDropped;
    }

    public static class AppliedCampaignLine {
        private String campaignUid;
        private String campaignName;
        private String offerLine;
        private BigDecimal pointsAwarded;
        private BigDecimal cashbackAwarded;

        public AppliedCampaignLine() {}

        public AppliedCampaignLine(
            String campaignUid,
            String campaignName,
            String offerLine,
            BigDecimal pointsAwarded,
            BigDecimal cashbackAwarded
        ) {
            this.campaignUid = campaignUid;
            this.campaignName = campaignName;
            this.offerLine = offerLine;
            this.pointsAwarded = pointsAwarded;
            this.cashbackAwarded = cashbackAwarded;
        }

        public String getCampaignUid() {
            return campaignUid;
        }

        public void setCampaignUid(String campaignUid) {
            this.campaignUid = campaignUid;
        }

        public String getCampaignName() {
            return campaignName;
        }

        public void setCampaignName(String campaignName) {
            this.campaignName = campaignName;
        }

        public String getOfferLine() {
            return offerLine;
        }

        public void setOfferLine(String offerLine) {
            this.offerLine = offerLine;
        }

        public BigDecimal getPointsAwarded() {
            return pointsAwarded;
        }

        public void setPointsAwarded(BigDecimal pointsAwarded) {
            this.pointsAwarded = pointsAwarded;
        }

        public BigDecimal getCashbackAwarded() {
            return cashbackAwarded;
        }

        public void setCashbackAwarded(BigDecimal cashbackAwarded) {
            this.cashbackAwarded = cashbackAwarded;
        }
    }
}
