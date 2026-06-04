package com.loyaltyos.referrals.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loyaltyos.referrals.model.ReferralLegacyConfigPayload;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferralConfigMigrationSupportTest {

    @Test
    void migratesEnabledBuiltinTypes() {
        ReferralProgrammeConfig config = new ReferralProgrammeConfig();
        config.setEnabledMilestoneTypes(List.of("SIGNUP", "FIRST_PURCHASE"));
        ReferralConfigMigrationSupport.migrateToMilestoneRules(config);
        assertEquals(2, config.getMilestoneRules().size());
        assertTrue(config.getMilestoneRules().stream().anyMatch(r -> "signup".equals(r.getKey())));
        assertTrue(
            config.getMilestoneRules().stream()
                .anyMatch(r -> "first_purchase".equals(r.getKey()) && r.getTrigger() == ReferralRuleTrigger.PURCHASE)
        );
    }

    @Test
    void migratesCustomLegacyPayload() {
        ReferralProgrammeConfig config = new ReferralProgrammeConfig();
        ReferralLegacyConfigPayload custom = new ReferralLegacyConfigPayload();
        custom.setKey("vip_signup");
        custom.setLabel("VIP sign-up");
        custom.setEvaluator("SIGNUP");
        config.setCustomMilestoneTypes(List.of(custom));
        ReferralConfigMigrationSupport.migrateToMilestoneRules(config);
        ReferralMilestoneRule rule = config.getMilestoneRules().get(0);
        assertEquals("vip_signup", rule.getKey());
        assertEquals(ReferralRuleTrigger.LINK, rule.getTrigger());
    }

    @Test
    void migratesStageConditionIntoRuleCriteria() {
        ReferralProgrammeConfig config = new ReferralProgrammeConfig();
        ReferralStageConfig stage = new ReferralStageConfig();
        stage.setStage(1);
        stage.setType("NTH_PURCHASE");
        stage.setCondition(Map.of("count", 3, "windowDays", 30));
        config.setStages(List.of(stage));
        ReferralConfigMigrationSupport.migrateToMilestoneRules(config);
        ReferralMilestoneRule rule = config.getMilestoneRules().stream()
            .filter(r -> "nth_purchase".equals(r.getKey()))
            .findFirst()
            .orElseThrow();
        assertEquals(3, rule.getCriteria().getMinPurchaseCount());
        assertEquals(30, rule.getCriteria().getWindowDays());
    }
}
