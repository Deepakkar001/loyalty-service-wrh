package com.loyaltyos.referrals.support;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferralConfigNormalizerTest {

    @Test
    void stripLegacyFieldsAfterNormalize() {
        ReferralProgrammeConfig config = new ReferralProgrammeConfig();
        config.setEnabledMilestoneTypes(List.of("SIGNUP"));
        ReferralStageConfig stage = new ReferralStageConfig();
        stage.setStage(1);
        stage.setType("SIGNUP");
        stage.setCondition(Map.of("count", 2));
        config.setStages(List.of(stage));

        ReferralConfigNormalizer.normalize(config);

        assertNull(config.getEnabledMilestoneTypes());
        assertNull(config.getCustomMilestoneTypes());
        assertNull(config.getStages().get(0).getCondition());
        assertTrue(config.getMilestoneRules() != null && !config.getMilestoneRules().isEmpty());
    }
}
