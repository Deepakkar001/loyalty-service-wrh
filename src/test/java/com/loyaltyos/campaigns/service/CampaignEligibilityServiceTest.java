package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.DropReason;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignTargetSegment;
import com.loyaltyos.campaigns.model.EligibilityResult;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignEligibilityServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignParticipationRepository participationRepository;

    private CampaignEligibilityService eligibilityService;
    private CampaignJsonSupport jsonSupport;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonSupport = new CampaignJsonSupport(objectMapper);
        eligibilityService = new CampaignEligibilityService(
            campaignRepository,
            participationRepository,
            jsonSupport
        );
    }

    @Test
    void targetSegment_parsesAndMatchesTierAndChannel() throws Exception {
        CampaignTargetSegment segment = jsonSupport.parseTargetSegment(objectMapper.readTree("""
            {"tierUids":["gold"],"channels":["APP"],"minAmount":500,"countries":["IN"]}
            """));
        assertTrue(segment.tierUids().contains("gold"));
        assertTrue(segment.channels().contains("APP"));
        assertEquals(0, new BigDecimal("500").compareTo(segment.minAmount()));
    }

    @Test
    void findQualifying_returnsActiveCampaignsPassingEmptySegment() {
        Campaign match = CampaignTestFixtures.campaign(
            "ok", "OK", 1, com.loyaltyos.campaigns.enums.StackMode.ADDITIVE, null,
            "POINTS_BONUS", new BigDecimal("10"), null, null
        );
        match.setTargetSegment(objectMapper.createObjectNode());

        when(campaignRepository.findActiveForEligibility(any(), any(), any(), any()))
            .thenReturn(List.of(match));

        EligibilityResult result = eligibilityService.findQualifying(
            "tenant-1",
            "default",
            new CampaignEventContext("cust-1", "gold", "PURCHASE", new BigDecimal("1000"), "APP", "IN")
        );

        assertEquals(1, result.qualifying().size());
        assertEquals("ok", result.qualifying().getFirst().getCampaignUid());
        assertTrue(result.dropped().isEmpty());
    }

    @Test
    void findQualifying_dropsWhenCustomerCapReached() {
        Campaign c = CampaignTestFixtures.campaign(
            "capped", "Capped", 1, com.loyaltyos.campaigns.enums.StackMode.ADDITIVE, null,
            "POINTS_BONUS", new BigDecimal("5"), null, null
        );
        c.setMaxPerCustomer(1);
        c.setTargetSegment(objectMapper.createObjectNode());

        when(campaignRepository.findActiveForEligibility(any(), any(), any(), any()))
            .thenReturn(List.of(c));
        when(participationRepository.countByTenantIdAndCampaignUidAndCustomerId("tenant-1", "capped", "cust-1"))
            .thenReturn(1L);

        EligibilityResult result = eligibilityService.findQualifying(
            "tenant-1",
            "default",
            new CampaignEventContext("cust-1", null, "PURCHASE", BigDecimal.TEN, null, null)
        );

        assertTrue(result.qualifying().isEmpty());
        assertEquals(DropReason.CUSTOMER_CAP_REACHED, result.dropped().getFirst().dropReason());
    }

    @Test
    void findQualifying_dropsWhenTierMismatch() throws Exception {
        Campaign c = CampaignTestFixtures.campaign(
            "tier-only", "Tier", 1, com.loyaltyos.campaigns.enums.StackMode.ADDITIVE, null,
            "POINTS_BONUS", new BigDecimal("5"), null, null
        );
        c.setTargetSegment(objectMapper.readTree("{\"tierUids\":[\"platinum\"]}"));

        when(campaignRepository.findActiveForEligibility(any(), any(), any(), any()))
            .thenReturn(List.of(c));

        EligibilityResult result = eligibilityService.findQualifying(
            "tenant-1",
            "default",
            new CampaignEventContext("cust-1", "gold", "PURCHASE", BigDecimal.TEN, null, null)
        );

        assertTrue(result.qualifying().isEmpty());
        assertEquals(DropReason.ELIGIBILITY_FAILED, result.dropped().getFirst().dropReason());
    }
}
