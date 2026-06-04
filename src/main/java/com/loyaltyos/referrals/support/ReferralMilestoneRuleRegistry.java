package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.dto.ReferralMilestoneTypeInfo;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ReferralMilestoneRuleRegistry {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,48}$");

    private ReferralMilestoneRuleRegistry() {}

    public static Optional<ReferralMilestoneRule> findRule(ReferralProgrammeConfig config, String key) {
        if (config == null || key == null || key.isBlank() || config.getMilestoneRules() == null) {
            return Optional.empty();
        }
        return config.getMilestoneRules().stream()
            .filter(r -> r != null && key.equalsIgnoreCase(r.getKey()))
            .findFirst();
    }

    public static ReferralMilestoneRule requireRule(ReferralProgrammeConfig config, ReferralStageConfig stage) {
        if (stage == null || stage.getType() == null || stage.getType().isBlank()) {
            throw new IllegalArgumentException("Stage missing milestone rule key");
        }
        return findRule(config, stage.getType().trim())
            .orElseThrow(() -> new IllegalArgumentException("Unknown milestone rule: " + stage.getType()));
    }

    public static boolean isLinkRule(ReferralProgrammeConfig config, ReferralStageConfig stage) {
        return findRule(config, stage.getType())
            .map(r -> r.getTrigger() == ReferralRuleTrigger.LINK)
            .orElse(false);
    }

    public static boolean isPurchaseRule(ReferralProgrammeConfig config, ReferralStageConfig stage) {
        return findRule(config, stage.getType())
            .map(r -> r.getTrigger() == ReferralRuleTrigger.PURCHASE)
            .orElse(false);
    }

    public static boolean isIntegrationEventRule(ReferralProgrammeConfig config, ReferralStageConfig stage) {
        return findRule(config, stage.getType())
            .map(r -> r.getTrigger() == ReferralRuleTrigger.INTEGRATION_EVENT)
            .orElse(false);
    }

    public static List<ReferralMilestoneTypeInfo> toDisplayList(ReferralProgrammeConfig config) {
        List<ReferralMilestoneTypeInfo> out = new ArrayList<>();
        if (config.getMilestoneRules() == null) {
            return out;
        }
        for (ReferralMilestoneRule rule : config.getMilestoneRules()) {
            if (rule == null || rule.getKey() == null) {
                continue;
            }
            ReferralMilestoneTypeInfo info = new ReferralMilestoneTypeInfo();
            info.setValue(rule.getKey());
            info.setLabel(rule.getLabel() != null ? rule.getLabel() : rule.getKey());
            info.setDescription(rule.getDescription());
            info.setTrigger(rule.getTrigger().name());
            info.setCustom(true);
            info.setEnabled(rule.isEnabled());
            out.add(info);
        }
        return out;
    }

    public static List<ReferralMilestoneRule> enabledRules(ReferralProgrammeConfig config) {
        if (config.getMilestoneRules() == null) {
            return List.of();
        }
        return config.getMilestoneRules().stream()
            .filter(r -> r != null && r.isEnabled() && r.getKey() != null && !r.getKey().isBlank())
            .toList();
    }

    public static boolean isValidKey(String key) {
        return key != null && KEY_PATTERN.matcher(key.trim().toLowerCase()).matches();
    }
}
