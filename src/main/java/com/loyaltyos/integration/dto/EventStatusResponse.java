package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class EventStatusResponse {

    private String eventId;
    private String status;
    private Instant timestamp;
    private Instant processedAt;
    private ResultSummary result;

    public static class ResultSummary {
        private BigDecimal pointsAwarded;
        private BigDecimal newBalance;
        private int rulesMatched;
        private int campaignsEligible;

        public BigDecimal getPointsAwarded() { return pointsAwarded; }
        public void setPointsAwarded(BigDecimal pointsAwarded) { this.pointsAwarded = pointsAwarded; }
        public BigDecimal getNewBalance() { return newBalance; }
        public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
        public int getRulesMatched() { return rulesMatched; }
        public void setRulesMatched(int rulesMatched) { this.rulesMatched = rulesMatched; }
        public int getCampaignsEligible() { return campaignsEligible; }
        public void setCampaignsEligible(int campaignsEligible) { this.campaignsEligible = campaignsEligible; }
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public ResultSummary getResult() { return result; }
    public void setResult(ResultSummary result) { this.result = result; }
}
