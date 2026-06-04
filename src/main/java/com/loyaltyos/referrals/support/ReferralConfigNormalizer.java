package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReferralConfigNormalizer {

    private ReferralConfigNormalizer() {}

    public static ReferralProgrammeConfig normalize(ReferralProgrammeConfig config) {
        if (config == null) {
            return new ReferralProgrammeConfig();
        }
        ReferralConfigMigrationSupport.migrateToMilestoneRules(config);

        if (config.getMilestoneRules() != null) {
            for (ReferralMilestoneRule rule : config.getMilestoneRules()) {
                if (rule.getKey() != null) {
                    rule.setKey(rule.getKey().trim().toLowerCase());
                }
                normalizeEventTypes(rule);
            }
        }

        List<ReferralMilestoneRule> enabled = ReferralMilestoneRuleRegistry.enabledRules(config);
        if (config.getStages() != null) {
            String fallback = enabled.isEmpty() ? null : enabled.get(0).getKey();
            for (ReferralStageConfig stage : config.getStages()) {
                if (stage.getType() == null || stage.getType().isBlank()) {
                    if (fallback != null) {
                        stage.setType(fallback);
                    }
                    continue;
                }
                String key = stage.getType().trim();
                Optional<ReferralMilestoneRule> match = ReferralMilestoneRuleRegistry.findRule(config, key);
                if (match.isPresent()) {
                    stage.setType(match.get().getKey());
                }
            }
        }

        stripLegacyFields(config);
        return config;
    }

    private static void normalizeEventTypes(ReferralMilestoneRule rule) {
        if (rule.getCriteria() == null || rule.getCriteria().getEventTypes() == null) {
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String raw : rule.getCriteria().getEventTypes()) {
            if (raw != null && !raw.isBlank()) {
                normalized.add(raw.trim().toUpperCase());
            }
        }
        rule.getCriteria().setEventTypes(normalized);
    }

    /** Removes deprecated keys so persisted config_json only contains the dynamic rule model. */
    public static void stripLegacyFields(ReferralProgrammeConfig config) {
        config.setEnabledMilestoneTypes(null);
        config.setCustomMilestoneTypes(null);
        if (config.getStages() != null) {
            for (ReferralStageConfig stage : config.getStages()) {
                stage.setCondition(null);
            }
        }
    }
}
