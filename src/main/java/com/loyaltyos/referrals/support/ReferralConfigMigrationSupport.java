package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.model.ReferralLegacyConfigPayload;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Upgrades legacy config (built-in type names + customMilestoneTypes) to {@link ReferralMilestoneRule}.
 */
public final class ReferralConfigMigrationSupport {

    private enum LegacyBuiltin {
        SIGNUP,
        FIRST_PURCHASE,
        NTH_PURCHASE,
        SPEND_THRESHOLD
    }

    private ReferralConfigMigrationSupport() {}

    public static void migrateToMilestoneRules(ReferralProgrammeConfig config) {
        if (config == null) {
            return;
        }
        if (config.getMilestoneRules() != null && !config.getMilestoneRules().isEmpty()) {
            return;
        }
        Map<String, ReferralMilestoneRule> rules = new LinkedHashMap<>();

        List<String> enabled = ReferralLegacyConfigPayload.copyEnabledKeys(config.getEnabledMilestoneTypes());
        for (String key : enabled) {
            parseLegacyBuiltin(key).ifPresent(builtin -> rules.putIfAbsent(builtinKey(builtin), legacyBuiltinRule(builtin, true)));
        }

        if (config.getCustomMilestoneTypes() != null) {
            for (ReferralLegacyConfigPayload custom : config.getCustomMilestoneTypes()) {
                if (custom == null || custom.getKey() == null) {
                    continue;
                }
                ReferralMilestoneRule rule = new ReferralMilestoneRule();
                rule.setKey(custom.getKey().trim().toLowerCase());
                rule.setLabel(custom.getLabel());
                rule.setDescription(custom.getDescription());
                rule.setEnabled(true);
                parseLegacyBuiltin(custom.getEvaluator()).ifPresentOrElse(
                    builtin -> {
                        rule.setTrigger(mapTrigger(builtin));
                        rule.setCriteria(mapCriteria(builtin, custom.getCondition()));
                    },
                    () -> {
                        rule.setTrigger(ReferralRuleTrigger.PURCHASE);
                        rule.setCriteria(mapCriteriaFromMap(custom.getCondition()));
                    }
                );
                rules.put(rule.getKey(), rule);
            }
        }

        if (rules.isEmpty() && config.getStages() != null) {
            for (ReferralStageConfig stage : config.getStages()) {
                if (stage.getType() == null) {
                    continue;
                }
                parseLegacyBuiltin(stage.getType()).ifPresent(builtin -> {
                    ReferralMilestoneRule rule = legacyBuiltinRule(builtin, true);
                    mergeStageCondition(rule, stage);
                    rules.putIfAbsent(builtinKey(builtin), rule);
                });
            }
        }

        if (rules.isEmpty()) {
            rules.put("signup", legacyBuiltinRule(LegacyBuiltin.SIGNUP, true));
            rules.put("first_purchase", legacyBuiltinRule(LegacyBuiltin.FIRST_PURCHASE, true));
        }

        config.setMilestoneRules(new ArrayList<>(rules.values()));
    }

    private static String builtinKey(LegacyBuiltin type) {
        return type.name().toLowerCase();
    }

    private static ReferralMilestoneRule legacyBuiltinRule(LegacyBuiltin type, boolean enabled) {
        ReferralMilestoneRule rule = new ReferralMilestoneRule();
        rule.setKey(builtinKey(type));
        rule.setLabel(type.name().replace('_', ' '));
        rule.setEnabled(enabled);
        rule.setTrigger(mapTrigger(type));
        rule.setCriteria(mapCriteria(type, Map.of()));
        return rule;
    }

    private static void mergeStageCondition(ReferralMilestoneRule rule, ReferralStageConfig stage) {
        if (stage.getCondition() == null || stage.getCondition().isEmpty()) {
            return;
        }
        ReferralRuleCriteria c = rule.getCriteria();
        applyConditionMap(c, stage.getCondition());
    }

    private static ReferralRuleTrigger mapTrigger(LegacyBuiltin type) {
        return type == LegacyBuiltin.SIGNUP ? ReferralRuleTrigger.LINK : ReferralRuleTrigger.PURCHASE;
    }

    private static ReferralRuleCriteria mapCriteria(LegacyBuiltin type, Map<String, Object> condition) {
        ReferralRuleCriteria c = mapCriteriaFromMap(condition);
        return switch (type) {
            case SIGNUP -> c;
            case FIRST_PURCHASE -> {
                c.setMinPurchaseCount(1);
                c.setFirstPurchaseOnly(true);
                yield c;
            }
            case NTH_PURCHASE -> {
                if (c.getMinPurchaseCount() == null) {
                    c.setMinPurchaseCount(1);
                }
                yield c;
            }
            case SPEND_THRESHOLD -> {
                if (c.getMinSpend() == null && condition != null && condition.containsKey("amount")) {
                    c.setMinSpend(new BigDecimal(String.valueOf(condition.get("amount"))));
                }
                if (c.getMinPurchaseCount() == null) {
                    c.setMinPurchaseCount(1);
                }
                yield c;
            }
        };
    }

    private static ReferralRuleCriteria mapCriteriaFromMap(Map<String, Object> condition) {
        ReferralRuleCriteria c = new ReferralRuleCriteria();
        if (condition == null) {
            return c;
        }
        applyConditionMap(c, condition);
        return c;
    }

    private static void applyConditionMap(ReferralRuleCriteria c, Map<String, Object> condition) {
        if (condition.containsKey("windowDays")) {
            c.setWindowDays(intVal(condition.get("windowDays")));
        }
        if (condition.containsKey("count")) {
            c.setMinPurchaseCount(intVal(condition.get("count")));
        }
        if (condition.containsKey("amount")) {
            c.setMinSpend(new BigDecimal(String.valueOf(condition.get("amount"))));
        }
        if (condition.containsKey("merchantId")) {
            c.setMerchantId(String.valueOf(condition.get("merchantId")));
        }
        if (condition.containsKey("category")) {
            c.setCategory(String.valueOf(condition.get("category")));
        }
        if (condition.containsKey("channel")) {
            c.setChannel(String.valueOf(condition.get("channel")));
        }
        if (condition.containsKey("sku")) {
            c.setSku(String.valueOf(condition.get("sku")));
        }
        if (condition.containsKey("region")) {
            c.setRegion(String.valueOf(condition.get("region")));
        }
    }

    private static Optional<LegacyBuiltin> parseLegacyBuiltin(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LegacyBuiltin.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static int intVal(Object o) {
        return Integer.parseInt(String.valueOf(o));
    }
}
