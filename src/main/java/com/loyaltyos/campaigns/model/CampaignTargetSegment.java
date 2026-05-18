package com.loyaltyos.campaigns.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignTargetSegment(
    @JsonProperty("tierUids") List<String> tierUids,
    @JsonProperty("channels") List<String> channels,
    @JsonProperty("minAmount") BigDecimal minAmount,
    @JsonProperty("countries") List<String> countries
) {
}
