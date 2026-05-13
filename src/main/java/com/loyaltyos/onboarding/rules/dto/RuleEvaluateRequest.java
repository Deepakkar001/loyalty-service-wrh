package com.loyaltyos.onboarding.rules.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class RuleEvaluateRequest {

    /** Defaults to {@code default} when omitted. */
    private String programmeUid = "default";

    @NotBlank
    private String customerId;

    /** Optional; when absent, lowest-rank tier from programme config is assumed. */
    private String customerTierUid;

    @NotBlank
    private String eventId;

    @NotBlank
    private String eventType;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private JsonNode eventPayload;

    private String channel;
    private String merchantId;
    private Long timestamp;

    public RuleEvaluateRequest() {}

    public RuleEvaluateRequest(
        String programmeUid,
        String customerId,
        String customerTierUid,
        String eventId,
        String eventType,
        BigDecimal amount,
        JsonNode eventPayload,
        String channel,
        String merchantId,
        Long timestamp
    ) {
        this.programmeUid = (programmeUid == null || programmeUid.isBlank()) ? "default" : programmeUid;
        this.customerId = customerId;
        this.customerTierUid = customerTierUid;
        this.eventId = eventId;
        this.eventType = eventType;
        this.amount = amount;
        this.eventPayload = eventPayload;
        this.channel = channel;
        this.merchantId = merchantId;
        this.timestamp = timestamp;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String programmeUid = "default";
        private String customerId;
        private String customerTierUid;
        private String eventId;
        private String eventType;
        private BigDecimal amount;
        private JsonNode eventPayload;
        private String channel;
        private String merchantId;
        private Long timestamp;

        private Builder() {}

        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder customerTierUid(String customerTierUid) { this.customerTierUid = customerTierUid; return this; }
        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder eventPayload(JsonNode eventPayload) { this.eventPayload = eventPayload; return this; }
        public Builder channel(String channel) { this.channel = channel; return this; }
        public Builder merchantId(String merchantId) { this.merchantId = merchantId; return this; }
        public Builder timestamp(Long timestamp) { this.timestamp = timestamp; return this; }

        public RuleEvaluateRequest build() {
            return new RuleEvaluateRequest(
                programmeUid,
                customerId,
                customerTierUid,
                eventId,
                eventType,
                amount,
                eventPayload,
                channel,
                merchantId,
                timestamp
            );
        }
    }

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerTierUid() { return customerTierUid; }
    public void setCustomerTierUid(String customerTierUid) { this.customerTierUid = customerTierUid; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public JsonNode getEventPayload() { return eventPayload; }
    public void setEventPayload(JsonNode eventPayload) { this.eventPayload = eventPayload; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
