package com.loyaltyos.campaigns.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CampaignParticipationResponse {

    private String campaignUid;
    private String programmeUid;
    private String customerId;
    private String eventId;
    private BigDecimal pointsAwarded;
    private BigDecimal cashbackAmount;
    private Instant participatedAt;

    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BigDecimal getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(BigDecimal pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public BigDecimal getCashbackAmount() { return cashbackAmount; }
    public void setCashbackAmount(BigDecimal cashbackAmount) { this.cashbackAmount = cashbackAmount; }
    public Instant getParticipatedAt() { return participatedAt; }
    public void setParticipatedAt(Instant participatedAt) { this.participatedAt = participatedAt; }
}
