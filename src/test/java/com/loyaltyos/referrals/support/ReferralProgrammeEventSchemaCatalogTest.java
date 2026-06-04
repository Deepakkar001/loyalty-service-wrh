package com.loyaltyos.referrals.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferralProgrammeEventSchemaCatalogTest {

    private static final String CONFIG = """
        {
          "eventSchema": {
            "eventDefinitions": [
              {
                "eventType": "PURCHASE",
                "coreFields": [
                  { "name": "amount", "type": "number", "required": true },
                  { "name": "merchantId", "type": "string", "required": false }
                ]
              },
              {
                "eventType": "PROFILE_COMPLETED",
                "coreFields": [
                  { "name": "loyaltyTier", "type": "string", "required": false }
                ]
              }
            ]
          }
        }
        """;

    @Test
    void parsesEventTypesAndFilterableFields() throws Exception {
        var root = new ObjectMapper().readTree(CONFIG);
        ReferralProgrammeEventSchemaCatalog catalog = ReferralProgrammeEventSchemaCatalog.fromProgrammeConfigJson(root);
        assertTrue(catalog.hasEventType("PURCHASE"));
        assertTrue(catalog.hasEventType("PROFILE_COMPLETED"));
        assertTrue(catalog.isAllowedMetadataKey(ReferralRuleTrigger.INTEGRATION_EVENT, "PROFILE_COMPLETED", "loyaltyTier"));
        assertFalse(catalog.isAllowedMetadataKey(ReferralRuleTrigger.INTEGRATION_EVENT, "PROFILE_COMPLETED", "unknownField"));
    }

    @Test
    void validatorRejectsUnknownMetadataFilter() throws Exception {
        var root = new ObjectMapper().readTree(CONFIG);
        ReferralProgrammeEventSchemaCatalog catalog = ReferralProgrammeEventSchemaCatalog.fromProgrammeConfigJson(root);

        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey("profile");
        rule.setLabel("Profile");
        rule.setEnabled(true);
        rule.setTrigger(ReferralRuleTrigger.INTEGRATION_EVENT);
        ReferralRuleCriteria c = new ReferralRuleCriteria();
        c.setEventTypes(List.of("PROFILE_COMPLETED"));
        c.getMetadataFilters().put("unknownField", "x");
        rule.setCriteria(c);

        org.junit.jupiter.api.Assertions.assertThrows(
            com.loyaltyos.referrals.exception.ReferralException.class,
            () -> ReferralConfigValidator.validate(minimalConfig(rule), catalog)
        );
    }

    private static com.loyaltyos.referrals.model.ReferralProgrammeConfig minimalConfig(ReferralMilestoneRule rule) {
        com.loyaltyos.referrals.model.ReferralProgrammeConfig config = new com.loyaltyos.referrals.model.ReferralProgrammeConfig();
        config.setMilestoneRules(List.of(rule));
        com.loyaltyos.referrals.model.ReferralStageConfig stage = new com.loyaltyos.referrals.model.ReferralStageConfig();
        stage.setStage(1);
        stage.setType(rule.getKey());
        config.setStages(List.of(stage));
        return config;
    }
}
