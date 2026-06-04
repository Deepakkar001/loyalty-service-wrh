package com.loyaltyos.referrals.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferralConfigValidatorTest {

    @Test
    void acceptsIntegrationEventRuleWithEventTypes() {
        ReferralProgrammeConfig config = validBase();
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("profile_done");
        rule.setLabel("Profile done");
        rule.setEnabled(true);
        rule.setTrigger(ReferralRuleTrigger.INTEGRATION_EVENT);
        ReferralRuleCriteria c = new ReferralRuleCriteria();
        c.setEventTypes(List.of("PROFILE_COMPLETED"));
        rule.setCriteria(c);
        config.setMilestoneRules(List.of(rule));
        config.getStages().get(0).setType("profile_done");
        assertDoesNotThrow(() -> ReferralConfigValidator.validate(config));
    }

    @Test
    void rejectsIntegrationEventWithoutEventTypes() {
        ReferralProgrammeConfig config = validBase();
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("profile_done");
        rule.setLabel("Profile done");
        rule.setEnabled(true);
        rule.setTrigger(ReferralRuleTrigger.INTEGRATION_EVENT);
        rule.setCriteria(new ReferralRuleCriteria());
        config.setMilestoneRules(List.of(rule));
        config.getStages().get(0).setType("profile_done");
        assertThrows(ReferralException.class, () -> ReferralConfigValidator.validate(config));
    }

    @Test
    void rejectsUnknownStageRuleKey() {
        ReferralProgrammeConfig config = validBase();
        config.getStages().get(0).setType("missing_rule");
        assertThrows(ReferralException.class, () -> ReferralConfigValidator.validate(config));
    }

    private static ReferralProgrammeConfig validBase() {
        ReferralProgrammeConfig config = new ReferralProgrammeConfig();
        ReferralMilestoneRule link = new ReferralMilestoneRule();
        link.setKey("signup");
        link.setLabel("Sign up");
        link.setEnabled(true);
        link.setTrigger(ReferralRuleTrigger.LINK);
        link.setCriteria(new ReferralRuleCriteria());
        config.setMilestoneRules(List.of(link));
        ReferralStageConfig stage = new ReferralStageConfig();
        stage.setStage(1);
        stage.setType("signup");
        config.setStages(List.of(stage));
        return config;
    }
}
