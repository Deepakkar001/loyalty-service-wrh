package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.DropReason;
import com.loyaltyos.campaigns.enums.StackMode;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignResolutionResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignConflictResolverTest {

    private CampaignConflictResolver resolver;
    private CampaignEventContext event;

    @BeforeEach
    void setUp() {
        resolver = new CampaignConflictResolver(new CampaignJsonSupport(new ObjectMapper()));
        event = new CampaignEventContext("cust-1", "gold", "PURCHASE", new BigDecimal("1000"), "APP", "IN");
    }

    @Test
    void firstMatch_keepsHighestPriorityInGroup() {
        Campaign high = CampaignTestFixtures.campaign(
            "c-high", "High", 10, StackMode.FIRST_MATCH, "g1",
            "POINTS_BONUS", new BigDecimal("50"), null, null
        );
        Campaign low = CampaignTestFixtures.campaign(
            "c-low", "Low", 1, StackMode.FIRST_MATCH, "g1",
            "POINTS_BONUS", new BigDecimal("200"), null, null
        );

        CampaignResolutionResult result = resolver.resolve(List.of(low, high), event);

        assertEquals(1, result.applying().size());
        assertEquals("c-high", result.applying().getFirst().getCampaignUid());
        assertEquals(1, result.dropped().size());
        assertEquals(DropReason.LOWER_PRIORITY_IN_GROUP, result.dropped().getFirst().dropReason());
        assertEquals("FIRST_MATCH", result.resolutionMode());
    }

    @Test
    void bestOffer_keepsHighestEstimatedValue() {
        Campaign small = CampaignTestFixtures.campaign(
            "c-small", "Small", 10, StackMode.BEST_OFFER, "g2",
            "POINTS_BONUS", new BigDecimal("10"), null, null
        );
        Campaign big = CampaignTestFixtures.campaign(
            "c-big", "Big", 1, StackMode.BEST_OFFER, "g2",
            "POINTS_BONUS", new BigDecimal("500"), null, null
        );

        CampaignResolutionResult result = resolver.resolve(List.of(small, big), event);

        assertEquals(1, result.applying().size());
        assertEquals("c-big", result.applying().getFirst().getCampaignUid());
        assertEquals(DropReason.LOWER_VALUE_IN_GROUP, result.dropped().getFirst().dropReason());
    }

    @Test
    void additive_keepsAllInGroup() {
        Campaign a = CampaignTestFixtures.campaign(
            "c-a", "A", 5, StackMode.ADDITIVE, "g3",
            "POINTS_BONUS", new BigDecimal("25"), null, null
        );
        Campaign b = CampaignTestFixtures.campaign(
            "c-b", "B", 3, StackMode.ADDITIVE, "g3",
            "POINTS_BONUS", new BigDecimal("30"), null, null
        );

        CampaignResolutionResult result = resolver.resolve(List.of(a, b), event);

        assertEquals(2, result.applying().size());
        assertTrue(result.dropped().isEmpty());
        assertEquals("ADDITIVE", result.resolutionMode());
    }

    @Test
    void multiGroup_stacksSurvivorsFromEachGroup() {
        Campaign g1 = CampaignTestFixtures.campaign(
            "g1-win", "G1", 5, StackMode.FIRST_MATCH, "group-one",
            "POINTS_BONUS", new BigDecimal("40"), null, null
        );
        Campaign g1Loser = CampaignTestFixtures.campaign(
            "g1-lose", "G1Lose", 1, StackMode.FIRST_MATCH, "group-one",
            "POINTS_BONUS", new BigDecimal("999"), null, null
        );
        Campaign g2a = CampaignTestFixtures.campaign(
            "g2a", "G2A", 2, StackMode.ADDITIVE, "group-two",
            "POINTS_BONUS", new BigDecimal("10"), null, null
        );
        Campaign g2b = CampaignTestFixtures.campaign(
            "g2b", "G2B", 1, StackMode.ADDITIVE, "group-two",
            "POINTS_BONUS", new BigDecimal("15"), null, null
        );

        CampaignResolutionResult result = resolver.resolve(List.of(g1Loser, g1, g2b, g2a), event);

        assertEquals(3, result.applying().size());
        assertEquals(1, result.dropped().size());
    }

    @Test
    void globalRewardCap_clipsProportionally() {
        Campaign a = CampaignTestFixtures.campaign(
            "cap-a", "CapA", 5, StackMode.ADDITIVE, "solo-a",
            "POINTS_BONUS", new BigDecimal("80"), null, new BigDecimal("100")
        );
        Campaign b = CampaignTestFixtures.campaign(
            "cap-b", "CapB", 4, StackMode.ADDITIVE, "solo-b",
            "POINTS_BONUS", new BigDecimal("80"), null, new BigDecimal("100")
        );

        CampaignResolutionResult result = resolver.resolve(List.of(a, b), event);

        assertTrue(result.capApplied());
        assertTrue(result.applying().size() <= 2);
        assertFalse(result.applying().isEmpty());
    }
}
