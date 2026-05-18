package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CampaignJsonSupport {

    private final ObjectMapper objectMapper;

    public CampaignJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public CampaignTargetSegment parseTargetSegment(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return new CampaignTargetSegment(null, null, null, null);
        }
        return objectMapper.convertValue(node, CampaignTargetSegment.class);
    }

    public CampaignOfferConfig parseOfferConfig(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return objectMapper.convertValue(node, CampaignOfferConfig.class);
    }
}
