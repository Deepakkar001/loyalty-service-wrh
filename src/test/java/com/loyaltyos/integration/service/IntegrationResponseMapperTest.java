package com.loyaltyos.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntegrationResponseMapperTest {

    private final IntegrationResponseMapper mapper = new IntegrationResponseMapper();

    @Test
    void earnings_useRecordedPreviousBalance_andEventCustomerFinalPoints() {
        LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
        core.setSuccess(true);
        core.setTotalPointsAwarded(new BigDecimal("0"));
        core.setReferralPointsToOtherCustomers(new BigDecimal("200"));
        core.setReferralPointsAwarded(new BigDecimal("200"));
        core.setPreviousBalance(new BigDecimal("50"));
        core.setNewBalance(new BigDecimal("50"));

        IntegrationParsedEvent request = new IntegrationParsedEvent(
            "PURCHASE",
            "evt-1",
            "default",
            "REFERRAL",
            null,
            "referee-002",
            BigDecimal.valueOf(500),
            null,
            Map.of("timestamp", "2026-06-03T10:00:00Z"),
            null,
            Map.of()
        );

        EventProcessingResponse response = mapper.toSuccessResponse(request, core, null, 10);

        assertThat(response.getEarnings().getFinalPoints()).isEqualByComparingTo("0");
        assertThat(response.getEarnings().getPreviousBalance()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getNewBalance()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getReferralPointsToOtherCustomers()).isEqualByComparingTo("200");
    }

    @Test
    void earnings_whenEventCustomerEarnsReferralPoints_balanceMathIsConsistent() {
        LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
        core.setSuccess(true);
        core.setTotalPointsAwarded(new BigDecimal("50"));
        core.setReferralPointsToOtherCustomers(new BigDecimal("200"));
        core.setReferralPointsAwarded(new BigDecimal("250"));
        core.setPreviousBalance(new BigDecimal("50"));
        core.setNewBalance(new BigDecimal("100"));

        IntegrationParsedEvent request = new IntegrationParsedEvent(
            "PURCHASE",
            "evt-2",
            "default",
            "REFERRAL",
            null,
            "referee-002",
            BigDecimal.valueOf(500),
            null,
            Map.of("timestamp", "2026-06-03T10:00:00Z"),
            null,
            Map.of()
        );

        EventProcessingResponse response = mapper.toSuccessResponse(request, core, null, 5);

        assertThat(response.getEarnings().getFinalPoints()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getPreviousBalance()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getNewBalance()).isEqualByComparingTo("100");
        assertThat(response.getEarnings().getReferralPointsToOtherCustomers()).isEqualByComparingTo("200");
    }

    @Test
    void customer_tierFieldsComeFromCoreOutcome() {
        LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
        core.setSuccess(true);
        core.setTotalPointsAwarded(new BigDecimal("4000"));
        core.setPreviousBalance(BigDecimal.ZERO);
        core.setNewBalance(new BigDecimal("4000"));
        core.setTierBeforeUid("uuid-bronze");
        core.setTierAfterUid("uuid-silver");
        core.setTierBeforeName("Bronze");
        core.setTierAfterName("Silver");
        core.setTierChanged(true);

        IntegrationParsedEvent request = new IntegrationParsedEvent(
            "PURCHASE",
            "evt-3",
            "default",
            null,
            null,
            "cust-1",
            BigDecimal.valueOf(500),
            null,
            Map.of(),
            null,
            Map.of()
        );

        EventProcessingResponse response = mapper.toSuccessResponse(request, core, null, 8);

        assertThat(response.getCustomer().getTierBefore()).isEqualTo("Bronze");
        assertThat(response.getCustomer().getTierAfter()).isEqualTo("Silver");
        assertThat(response.getCustomer().isTierChanged()).isTrue();
    }

    @Test
    void rules_useRealMatchedRulesFromRuleEvaluation() {
        LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
        core.setSuccess(true);
        core.setRulePointsAwarded(new BigDecimal("1200"));
        core.setTotalPointsAwarded(new BigDecimal("1200"));
        core.setPreviousBalance(new BigDecimal("4000"));
        core.setNewBalance(new BigDecimal("5200"));

        RuleEvaluationResponse ruleEval = new RuleEvaluationResponse();
        ruleEval.setBasePointsCalculated(new BigDecimal("1000"));
        ruleEval.setTierMultiplier(new BigDecimal("1.2"));
        ruleEval.setFinalPointsAwarded(new BigDecimal("1200"));
        RuleEvaluationResponse.MatchedRuleInfo matched = RuleEvaluationResponse.MatchedRuleInfo.builder()
            .ruleUid("c239a0bd-8a4e-403f-84f7-87575e3ed436")
            .ruleName("Purchase earn rule")
            .priority(10)
            .pointsFromThisRule(new BigDecimal("1200"))
            .build();
        ruleEval.setMatchedRules(List.of(matched));
        core.setRuleEvaluation(ruleEval);

        IntegrationParsedEvent request = new IntegrationParsedEvent(
            "PURCHASE",
            "evt-4",
            "default",
            null,
            null,
            "cust-1",
            BigDecimal.valueOf(500),
            null,
            Map.of(),
            null,
            Map.of()
        );

        EventProcessingResponse response = mapper.toSuccessResponse(request, core, ruleEval, 12);

        assertThat(response.getRules().getMatched()).hasSize(1);
        assertThat(response.getRules().getMatched().getFirst().getRuleId())
            .isEqualTo("c239a0bd-8a4e-403f-84f7-87575e3ed436");
        assertThat(response.getRules().getMatched().getFirst().getRuleName()).isEqualTo("Purchase earn rule");
        assertThat(response.getRules().getMatched().getFirst().getPriority()).isEqualTo(10);
        assertThat(response.getRules().getMatched().getFirst().getPointsAwarded()).isEqualByComparingTo("1200");
        assertThat(response.getRules().getMatched().getFirst().getMultiplier()).isEqualByComparingTo("1.2");
        assertThat(response.getEarnings().getBasePoints()).isEqualByComparingTo("1000");
        assertThat(response.getEarnings().getTierMultiplier()).isEqualByComparingTo("1.2");
    }
}
