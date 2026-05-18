package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.StackMode;
import java.math.BigDecimal;
import java.time.Instant;

final class CampaignTestFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CampaignTestFixtures() {}

    static Campaign campaign(
        String uid,
        String name,
        int priority,
        StackMode stackMode,
        String mutualExclGroup,
        String awardType,
        BigDecimal bonusPoints,
        BigDecimal multiplier,
        BigDecimal globalCap
    ) {
        Campaign c = new Campaign();
        c.setTenantId("tenant-1");
        c.setProgrammeUid("default");
        c.setCampaignUid(uid);
        c.setName(name);
        c.setCampaignType("TEST");
        c.setPriority(priority);
        c.setStackMode(stackMode);
        c.setMutualExclGroup(mutualExclGroup);
        c.setBudgetTotal(new BigDecimal("10000"));
        c.setBudgetConsumed(BigDecimal.ZERO);
        c.setGlobalRewardCap(globalCap);
        c.setValidFrom(Instant.parse("2020-01-01T00:00:00Z"));
        c.setValidUntil(Instant.parse("2030-01-01T00:00:00Z"));
        c.setTriggerEventType("PURCHASE");

        ObjectNode offer = MAPPER.createObjectNode();
        offer.put("awardType", awardType);
        if (bonusPoints != null) {
            offer.put("bonusPoints", bonusPoints);
        }
        if (multiplier != null) {
            offer.put("multiplierOnRulePoints", multiplier);
        }
        c.setOfferConfig(offer);
        return c;
    }
}
