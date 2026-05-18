package com.loyaltyos.campaigns.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;

public class LoyaltyEventProcessRequest {

    private String programmeUid = "default";

    @NotBlank
    private String customerId;

    private String customerTierUid;

    @NotBlank
    private String eventType;

    @NotBlank
    private String transactionId;

    @NotNull
    @Positive
    private BigDecimal amount;

  /** Optional channel, country, and other metadata echoed into validation payload. */
    private Map<String, Object> metadata;

    private JsonNode eventPayload;

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

    public String getCustomerTierUid() {
        return customerTierUid;
    }

    public void setCustomerTierUid(String customerTierUid) {
        this.customerTierUid = customerTierUid;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public JsonNode getEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(JsonNode eventPayload) {
        this.eventPayload = eventPayload;
    }
}
