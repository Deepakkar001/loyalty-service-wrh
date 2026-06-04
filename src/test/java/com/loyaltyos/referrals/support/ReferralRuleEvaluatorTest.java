package com.loyaltyos.referrals.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.support.ReferralProgressTracker.ProgressState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferralRuleEvaluatorTest {

    @Test
    void linkTriggerAlwaysSatisfied() {
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("signup");
        rule.setTrigger(ReferralRuleTrigger.LINK);
        rule.setCriteria(new ReferralRuleCriteria());
        Referral referral = new Referral();
        assertTrue(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
    }

    @Test
    void minPurchaseCountUsesReferralCounter() {
        ReferralMilestoneRule rule = purchaseRule(2, false, null);
        Referral referral = new Referral();
        referral.setPurchaseCount(1);
        assertFalse(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
        referral.setPurchaseCount(2);
        assertTrue(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
    }

    @Test
    void minSpendUsesTotalSpendOnReferral() {
        ReferralMilestoneRule rule = purchaseRule(1, false, BigDecimal.valueOf(100));
        Referral referral = new Referral();
        referral.setPurchaseCount(1);
        referral.setTotalSpend(BigDecimal.valueOf(50));
        assertFalse(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
        referral.setTotalSpend(BigDecimal.valueOf(120));
        assertTrue(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
    }

    @Test
    void integrationEventMatchesMetadataFilter() {
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("profile");
        rule.setTrigger(ReferralRuleTrigger.INTEGRATION_EVENT);
        ReferralRuleCriteria c = new ReferralRuleCriteria();
        c.setEventTypes(List.of("PROFILE_COMPLETED"));
        c.getMetadataFilters().put("loyaltyTier", "GOLD");
        rule.setCriteria(c);
        assertTrue(
            ReferralRuleEvaluator.isSatisfiedForIntegrationEvent(
                rule,
                "PROFILE_COMPLETED",
                Map.of("loyaltyTier", "GOLD")
            )
        );
        assertFalse(
            ReferralRuleEvaluator.isSatisfiedForIntegrationEvent(
                rule,
                "PROFILE_COMPLETED",
                Map.of("loyaltyTier", "SILVER")
            )
        );
    }

    @Test
    void integrationEventMatchesConfiguredType() {
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("profile");
        rule.setTrigger(ReferralRuleTrigger.INTEGRATION_EVENT);
        ReferralRuleCriteria c = new ReferralRuleCriteria();
        c.setEventTypes(List.of("PROFILE_COMPLETED"));
        rule.setCriteria(c);
        assertTrue(
            ReferralRuleEvaluator.isSatisfiedForIntegrationEvent(rule, "PROFILE_COMPLETED", Map.of())
        );
        assertFalse(
            ReferralRuleEvaluator.isSatisfiedForIntegrationEvent(rule, "PURCHASE", Map.of())
        );
    }

    @Test
    void firstPurchaseOnlyAcceptsFirstPurchase() {
        ReferralMilestoneRule rule = purchaseRule(1, true, null);
        Referral referral = new Referral();
        referral.setPurchaseCount(0);
        assertFalse(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
        referral.setPurchaseCount(1);
        assertTrue(ReferralRuleEvaluator.isSatisfied(rule, referral, new ProgressState(), Instant.now()));
    }

    private static ReferralMilestoneRule purchaseRule(int minCount, boolean firstOnly, BigDecimal minSpend) {
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("purchase");
        rule.setTrigger(ReferralRuleTrigger.PURCHASE);
        ReferralRuleCriteria c = new ReferralRuleCriteria();
        c.setMinPurchaseCount(minCount);
        c.setFirstPurchaseOnly(firstOnly);
        c.setMinSpend(minSpend);
        rule.setCriteria(c);
        return rule;
    }
}
