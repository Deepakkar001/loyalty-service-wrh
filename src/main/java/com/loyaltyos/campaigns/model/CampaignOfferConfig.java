package com.loyaltyos.campaigns.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignOfferConfig(
    @JsonProperty("awardType") String awardType,
    @JsonProperty("multiplierOnRulePoints") BigDecimal multiplierOnRulePoints,
    @JsonProperty("bonusPoints") BigDecimal bonusPoints,
    @JsonProperty("cashbackValue") BigDecimal cashbackValue,
    @JsonProperty("expiryDays") Integer expiryDays,
    @JsonProperty("stackableWithRules") Boolean stackableWithRules
) {
    public boolean isStackableWithRules() {
        return stackableWithRules == null || stackableWithRules;
    }
}
