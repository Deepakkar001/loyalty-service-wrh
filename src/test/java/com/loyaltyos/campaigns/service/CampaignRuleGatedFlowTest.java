package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignExecutionMode;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.repository.CampaignRuleSandboxPassRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetCustomerRepository;
import com.loyaltyos.rules.entity.EarnRule;
import com.loyaltyos.rules.enums.RuleStatus;
import com.loyaltyos.rules.enums.RuleType;
import com.loyaltyos.rules.repository.EarnRuleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignRuleGatedFlowTest {

    @Mock private EarnRuleRepository earnRuleRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignRuleSandboxPassRepository sandboxPassRepository;
    @Mock private CampaignTargetCustomerRepository targetCustomerRepository;
    @Mock private CampaignParticipationRepository participationRepository;

    private CampaignRuleSandboxService sandboxService;
    private CampaignProperties campaignProperties;

    @BeforeEach
    void setUp() {
        sandboxService = new CampaignRuleSandboxService(
            sandboxPassRepository,
            campaignRepository,
            targetCustomerRepository,
            earnRuleRepository
        );
        campaignProperties = new CampaignProperties();
        campaignProperties.setRuleGatedOnly(true);
    }

    @Test
    void targetedSandbox_wrongCustomerFailsCheck() {
        Campaign campaign = CampaignTestFixtures.campaign(
            "c1", "Targeted", 1,
            com.loyaltyos.campaigns.enums.StackMode.ADDITIVE,
            null, "POINTS_BONUS", java.math.BigDecimal.TEN, null, null
        );
        campaign.setCustomerScope(CustomerScope.TARGETED);

        when(targetCustomerRepository.existsByTenantIdAndCampaignUidAndCustomerId("t1", "c1", "wrong"))
            .thenReturn(false);

        var result = sandboxService.checkTargetedCustomer("t1", campaign, "wrong");
        assertFalse(result.passed());
        assertEquals("TARGETED_CUSTOMER_NOT_IN_LIST", result.errorCode());
    }

    @Test
    void targetedSandbox_listedCustomerPassesCheck() {
        Campaign campaign = CampaignTestFixtures.campaign(
            "c1", "Targeted", 1,
            com.loyaltyos.campaigns.enums.StackMode.ADDITIVE,
            null, "POINTS_BONUS", java.math.BigDecimal.TEN, null, null
        );
        campaign.setCustomerScope(CustomerScope.TARGETED);

        when(targetCustomerRepository.existsByTenantIdAndCampaignUidAndCustomerId("t1", "c1", "cust_a"))
            .thenReturn(true);

        assertTrue(sandboxService.checkTargetedCustomer("t1", campaign, "cust_a").passed());
    }

    @Test
    void allAudienceSandbox_skipsTargetCheck() {
        Campaign campaign = CampaignTestFixtures.campaign(
            "c1", "All", 1,
            com.loyaltyos.campaigns.enums.StackMode.ADDITIVE,
            null, "POINTS_BONUS", java.math.BigDecimal.TEN, null, null
        );
        campaign.setCustomerScope(CustomerScope.ALL);

        assertTrue(sandboxService.checkTargetedCustomer("t1", campaign, "anyone").passed());
    }

    @Test
    void activateCampaign_withoutActiveRule_throwsCampaignRuleRequired() {
        Campaign campaign = CampaignTestFixtures.campaign(
            "c1", "Promo", 1,
            com.loyaltyos.campaigns.enums.StackMode.ADDITIVE,
            null, "POINTS_BONUS", java.math.BigDecimal.valueOf(1000), null, null
        );
        campaign.setExecutionMode(CampaignExecutionMode.RULE_GATED);
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setCustomerScope(CustomerScope.ALL);

        when(earnRuleRepository.findByTenantIdAndCampaignUidAndRuleTypeAndStatus(
            eq("t1"), eq("c1"), eq(RuleType.CAMPAIGN), eq(RuleStatus.ACTIVE)
        )).thenReturn(List.of());
        when(earnRuleRepository.findByTenantIdAndCampaignUidAndRuleTypeOrderByPriorityDesc(
            eq("t1"), eq("c1"), eq(RuleType.CAMPAIGN)
        )).thenReturn(List.of());

        CampaignService service = buildCampaignService();
        when(campaignRepository.findByTenantIdAndCampaignUid("t1", "c1")).thenReturn(Optional.of(campaign));

        CampaignConflictException ex = assertThrows(
            CampaignConflictException.class,
            () -> service.activate("t1", "c1")
        );
        assertEquals("CAMPAIGN_RULE_REQUIRED", ex.getErrorCode());
    }

    @Test
    void activateCampaign_withoutSandbox_throwsSandboxRequired() {
        Campaign campaign = CampaignTestFixtures.campaign(
            "c1", "Promo", 1,
            com.loyaltyos.campaigns.enums.StackMode.ADDITIVE,
            null, "POINTS_BONUS", java.math.BigDecimal.valueOf(1000), null, null
        );
        campaign.setExecutionMode(CampaignExecutionMode.RULE_GATED);
        campaign.setStatus(CampaignStatus.DRAFT);

        EarnRule rule = new EarnRule();
        rule.setRuleUid("rule-1");
        rule.setRuleType(RuleType.CAMPAIGN);
        rule.setCampaignUid("c1");
        rule.setStatus(RuleStatus.ACTIVE);

        when(campaignRepository.findByTenantIdAndCampaignUid("t1", "c1")).thenReturn(Optional.of(campaign));
        when(earnRuleRepository.findByTenantIdAndCampaignUidAndRuleTypeAndStatus(
            eq("t1"), eq("c1"), eq(RuleType.CAMPAIGN), eq(RuleStatus.ACTIVE)
        )).thenReturn(List.of(rule));
        when(sandboxPassRepository.existsByTenantIdAndRuleUid("t1", "rule-1")).thenReturn(false);

        CampaignService service = buildCampaignService();

        CampaignConflictException ex = assertThrows(
            CampaignConflictException.class,
            () -> service.activate("t1", "c1")
        );
        assertEquals("SANDBOX_REQUIRED", ex.getErrorCode());
    }

    private CampaignService buildCampaignService() {
        return new CampaignService(
            campaignRepository,
            org.mockito.Mockito.mock(CampaignAnalyticsService.class),
            org.mockito.Mockito.mock(CampaignProgrammeValidator.class),
            campaignProperties,
            new ObjectMapper(),
            org.mockito.Mockito.mock(com.loyaltyos.onboarding.service.ProgrammeService.class),
            earnRuleRepository,
            sandboxService
        );
    }
}
