package com.loyaltyos.campaigns.dto;

import java.time.Instant;

public class CampaignTargetCustomerResponse {

    private String customerId;
    private Instant addedAt;
    private String addedBy;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
}
