package com.loyaltyos.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.service.CampaignRuleSandboxService;
import com.loyaltyos.campaigns.service.CampaignService;
import com.loyaltyos.integration.service.IntegrationCredentialCryptoService;
import com.loyaltyos.integration.service.IntegrationEventPayloadResolver;
import com.loyaltyos.onboarding.dto.SandboxValidateEventRequest;
import com.loyaltyos.onboarding.entity.TenantConfig;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.SandboxTestEventRepository;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import com.loyaltyos.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.rules.entity.EarnRule;
import com.loyaltyos.rules.enums.RuleType;
import com.loyaltyos.rules.service.RuleEvaluationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceSandboxTargetedTest {

    @Mock private TenantOnboardingRepository tenantOnboardingRepository;
    @Mock private TenantApiKeyRepository tenantApiKeyRepository;
    @Mock private TenantConfigRepository tenantConfigRepository;
    @Mock private OnboardingAuditLogRepository auditLogRepository;
    @Mock private OnboardingStateMachine stateMachine;
    @Mock private RuleEvaluationService ruleEvaluationService;
    @Mock private SandboxTestEventRepository sandboxTestEventRepository;
    @Mock private ProgrammeService programmeService;
    @Mock private IntegrationCredentialCryptoService credentialCryptoService;
    @Mock private IntegrationEventPayloadResolver integrationEventPayloadResolver;
    @Mock private CampaignRuleSandboxService campaignRuleSandboxService;
    @Mock private CampaignService campaignService;

    private IntegrationService integrationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        integrationService = new IntegrationService(
            tenantOnboardingRepository,
            tenantApiKeyRepository,
            tenantConfigRepository,
            auditLogRepository,
            stateMachine,
            objectMapper,
            ruleEvaluationService,
            sandboxTestEventRepository,
            programmeService,
            credentialCryptoService,
            integrationEventPayloadResolver,
            campaignRuleSandboxService,
            campaignService
        );
    }

    @Test
    void validateSandboxEvent_nonTargetedCustomer_skipsRuleEvaluation() {
        String ruleUid = "20e60af7-da9c-4658-a31a-02b93da06a12";
        Campaign campaign = new Campaign();
        campaign.setCampaignUid("camp-1");
        campaign.setCustomerScope(CustomerScope.TARGETED);
        EarnRule rule = new EarnRule();
        rule.setRuleUid(ruleUid);
        rule.setRuleType(RuleType.CAMPAIGN);
        rule.setCampaignUid("camp-1");

        when(tenantConfigRepository.findByTenantId("t1")).thenReturn(Optional.of(new TenantConfig()));
        when(campaignRuleSandboxService.resolveCampaignRule(eq("t1"), eq(ruleUid), any()))
            .thenReturn(new CampaignRuleSandboxService.ResolvedCampaignRule(rule, campaign));
        when(campaignRuleSandboxService.checkTargetedCustomer(eq("t1"), eq(campaign), eq("outsider")))
            .thenReturn(CampaignRuleSandboxService.TargetedCustomerCheckResult.fail(
                "TARGETED_CUSTOMER_NOT_IN_LIST",
                "customerId is not in the campaign target list"
            ));

        SandboxValidateEventRequest request = new SandboxValidateEventRequest();
        request.setRuleUid(ruleUid);
        request.setPayloadJson("""
            {
              "eventType": "PURCHASE",
              "customerId": "outsider",
              "transactionId": "txn-1",
              "timestamp": "2026-06-08T10:00:00Z",
              "amount": 100
            }
            """);

        Map<String, Object> result = integrationService.validateSandboxEvent("t1", request);

        assertEquals(Boolean.FALSE, result.get("targetedCustomerOk"));
        assertEquals(Boolean.TRUE, result.get("ruleEvaluationSkipped"));
        assertEquals(Boolean.FALSE, result.get("sandboxPassed"));

        @SuppressWarnings("unchecked")
        Map<String, Object> ruleEval = (Map<String, Object>) result.get("ruleEvaluation");
        assertEquals("No match", ruleEval.get("message"));
        assertEquals(0, ((List<?>) ruleEval.get("matchedRules")).size());
        assertEquals(0, ((Number) ruleEval.get("finalPointsAwarded")).intValue());

        verify(ruleEvaluationService, never()).evaluateSingleRule(any(), any(), any());
        verify(ruleEvaluationService, never()).evaluate(any(), any());
    }

    @Test
    void validateSandboxEvent_targetedCustomer_proceedsToRuleEvaluation() {
        String ruleUid = "rule-listed";
        Campaign campaign = new Campaign();
        campaign.setCampaignUid("camp-1");
        campaign.setCustomerScope(CustomerScope.TARGETED);
        EarnRule rule = new EarnRule();
        rule.setRuleUid(ruleUid);
        rule.setRuleType(RuleType.CAMPAIGN);
        rule.setCampaignUid("camp-1");

        RuleEvaluationResponse evalResponse = RuleEvaluationResponse.builder()
            .success(true)
            .message("Evaluation complete. 1 rule(s) matched.")
            .finalPointsAwarded(new BigDecimal("5"))
            .matchedRules(List.of())
            .build();

        when(tenantConfigRepository.findByTenantId("t1")).thenReturn(Optional.of(new TenantConfig()));
        when(campaignRuleSandboxService.resolveCampaignRule(eq("t1"), eq(ruleUid), any()))
            .thenReturn(new CampaignRuleSandboxService.ResolvedCampaignRule(rule, campaign));
        when(campaignRuleSandboxService.checkTargetedCustomer(eq("t1"), eq(campaign), eq("cust_a")))
            .thenReturn(CampaignRuleSandboxService.TargetedCustomerCheckResult.success());
        when(integrationEventPayloadResolver.buildRuleEvaluateRequest(any())).thenReturn(new RuleEvaluateRequest());
        when(ruleEvaluationService.evaluateSingleRule(eq("t1"), eq(ruleUid), any())).thenReturn(evalResponse);

        SandboxValidateEventRequest request = new SandboxValidateEventRequest();
        request.setRuleUid(ruleUid);
        request.setPayloadJson("""
            {
              "eventType": "PURCHASE",
              "customerId": "cust_a",
              "transactionId": "txn-2",
              "timestamp": "2026-06-08T10:00:00Z",
              "amount": 100
            }
            """);

        Map<String, Object> result = integrationService.validateSandboxEvent("t1", request);

        assertEquals(Boolean.TRUE, result.get("targetedCustomerOk"));
        assertFalse(result.containsKey("ruleEvaluationSkipped"));
        verify(ruleEvaluationService).evaluateSingleRule(eq("t1"), eq(ruleUid), any());
    }
}
