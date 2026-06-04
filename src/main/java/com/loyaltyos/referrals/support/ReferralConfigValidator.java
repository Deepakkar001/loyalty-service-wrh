package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralCapRule;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralPartyRewardConfig;
import com.loyaltyos.referrals.model.ReferralPointsBudget;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.model.ReferralRewardType;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReferralConfigValidator {

    private ReferralConfigValidator() {}

    public static void validate(ReferralProgrammeConfig config) {
        validate(config, null);
    }

    public static void validate(ReferralProgrammeConfig config, ReferralProgrammeEventSchemaCatalog programmeSchema) {
        ReferralProgrammeConfig normalized = ReferralConfigNormalizer.normalize(config);
        List<ReferralMilestoneRule> enabled = ReferralMilestoneRuleRegistry.enabledRules(normalized);
        if (enabled.isEmpty()) {
            throw new ReferralException("INVALID_CONFIG", "At least one enabled milestone rule is required");
        }
        validateRules(normalized);
        if (normalized.getStages() == null || normalized.getStages().isEmpty()) {
            throw new ReferralException("INVALID_CONFIG", "At least one referral stage is required");
        }
        Set<Integer> stageNumbers = new HashSet<>();
        Set<String> ruleKeys = new HashSet<>();
        for (ReferralMilestoneRule rule : normalized.getMilestoneRules()) {
            if (rule != null && rule.getKey() != null) {
                ruleKeys.add(rule.getKey().toLowerCase());
            }
        }
        for (ReferralStageConfig stage : normalized.getStages()) {
            if (stage.getStage() < 1) {
                throw new ReferralException("INVALID_CONFIG", "Stage number must be >= 1");
            }
            if (!stageNumbers.add(stage.getStage())) {
                throw new ReferralException("INVALID_CONFIG", "Duplicate stage number: " + stage.getStage());
            }
            if (stage.getType() == null || stage.getType().isBlank()) {
                throw new ReferralException("INVALID_CONFIG", "Stage " + stage.getStage() + " requires a milestone rule");
            }
            String typeKey = stage.getType().trim().toLowerCase();
            if (!ruleKeys.contains(typeKey)) {
                throw new ReferralException(
                    "INVALID_CONFIG",
                    "Stage " + stage.getStage() + " references unknown rule: " + typeKey
                );
            }
            ReferralMilestoneRule rule = ReferralMilestoneRuleRegistry.findRule(normalized, typeKey)
                .orElseThrow(() -> new ReferralException("INVALID_CONFIG", "Unknown rule: " + typeKey));
            if (!rule.isEnabled()) {
                throw new ReferralException(
                    "INVALID_CONFIG",
                    "Stage " + stage.getStage() + " uses disabled rule: " + typeKey
                );
            }
            validatePoints(stage.getReferrerPoints(), "referrerPoints", stage.getStage());
            validatePoints(stage.getRefereePoints(), "refereePoints", stage.getStage());
            validatePartyReward(stage.getReferrerReward(), "referrerReward", stage.getStage());
            validatePartyReward(stage.getRefereeReward(), "refereeReward", stage.getStage());
            if (stage.getMaxReferrerAwardsForStage() != null && stage.getMaxReferrerAwardsForStage() < 1) {
                throw new ReferralException("INVALID_CONFIG", "maxReferrerAwardsForStage must be >= 1");
            }
            validateRuleCriteria(rule);
        }
        if (normalized.getPointsBudget() != null) {
            validatePointsBudget(normalized.getPointsBudget());
        }
        if (normalized.getCapRules() != null) {
            for (ReferralCapRule rule : normalized.getCapRules()) {
                if (rule.getType() == null) {
                    throw new ReferralException("INVALID_CONFIG", "Cap rule requires type");
                }
                if (rule.getType() == ReferralCapRule.CapType.ROLLING_DAY_REFERRALS
                    && rule.getWindowDays() != null
                    && rule.getWindowDays() < 1) {
                    throw new ReferralException("INVALID_CONFIG", "windowDays must be >= 1 for rolling cap");
                }
            }
        }
        if (normalized.getFraudPolicy() != null && normalized.getFraudPolicy().getMaxReferralsPer24Hours() < 1) {
            throw new ReferralException("INVALID_CONFIG", "maxReferralsPer24Hours must be >= 1");
        }
        if (programmeSchema != null && !programmeSchema.getEventTypes().isEmpty()) {
            validateRulesAgainstProgrammeSchema(normalized, programmeSchema);
        }
    }

    private static void validateRulesAgainstProgrammeSchema(
        ReferralProgrammeConfig config,
        ReferralProgrammeEventSchemaCatalog schema
    ) {
        for (ReferralMilestoneRule rule : config.getMilestoneRules()) {
            if (rule == null) {
                continue;
            }
            validateRuleAgainstProgrammeSchema(rule, schema);
        }
    }

    private static void validateRuleAgainstProgrammeSchema(
        ReferralMilestoneRule rule,
        ReferralProgrammeEventSchemaCatalog schema
    ) {
        String ruleKey = rule.getKey();
        ReferralRuleCriteria c = rule.getCriteria();
        if (rule.getTrigger() == ReferralRuleTrigger.LINK) {
            if (hasAnyCriteriaSet(c)) {
                throw new ReferralException(
                    "INVALID_CONFIG",
                    "Rule " + ruleKey + " (referral link) cannot have purchase or event filters"
                );
            }
            return;
        }

        if (rule.getTrigger() == ReferralRuleTrigger.INTEGRATION_EVENT) {
            for (String et : c.getEventTypes()) {
                if (et != null && !et.isBlank() && !schema.hasEventType(et)) {
                    throw new ReferralException(
                        "INVALID_CONFIG",
                        "Rule " + ruleKey + " references event type not in programme schema: " + et.trim()
                    );
                }
            }
        }

        if (rule.getTrigger() == ReferralRuleTrigger.PURCHASE && !schema.hasEventType("PURCHASE")) {
            throw new ReferralException(
                "INVALID_CONFIG",
                "Rule " + ruleKey + " uses PURCHASE but programme event schema has no PURCHASE event"
            );
        }

        String primaryEvent = primaryEventType(rule);
        assertAllowed(c, "merchantId", rule, schema, primaryEvent);
        assertAllowed(c, "category", rule, schema, primaryEvent);
        assertAllowed(c, "channel", rule, schema, primaryEvent);
        assertAllowed(c, "sku", rule, schema, primaryEvent);
        assertAllowed(c, "region", rule, schema, primaryEvent);
        assertAllowed(c, "minPurchaseCount", rule, schema, primaryEvent);
        assertAllowed(c, "minSpend", rule, schema, primaryEvent);
        assertAllowed(c, "windowDays", rule, schema, primaryEvent);
        assertAllowed(c, "firstPurchaseOnly", rule, schema, primaryEvent);

        if (c.getMetadataFilters() != null) {
            for (Map.Entry<String, String> entry : c.getMetadataFilters().entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                if (!schema.isAllowedMetadataKey(rule.getTrigger(), primaryEvent, entry.getKey())) {
                    throw new ReferralException(
                        "INVALID_CONFIG",
                        "Rule " + ruleKey + " filter '" + entry.getKey() + "' is not defined on event " + primaryEvent
                    );
                }
            }
        }
    }

    private static void assertAllowed(
        ReferralRuleCriteria c,
        String field,
        ReferralMilestoneRule rule,
        ReferralProgrammeEventSchemaCatalog schema,
        String primaryEvent
    ) {
        if (!isCriteriaFieldSet(c, field)) {
            return;
        }
        if (!schema.isAllowedCriteriaKey(rule.getTrigger(), primaryEvent, field)) {
            throw new ReferralException(
                "INVALID_CONFIG",
                "Rule " + rule.getKey() + " field '" + field + "' is not allowed for event " + primaryEvent
            );
        }
    }

    private static boolean isCriteriaFieldSet(ReferralRuleCriteria c, String field) {
        if (c == null) {
            return false;
        }
        return switch (field) {
            case "merchantId" -> c.getMerchantId() != null && !c.getMerchantId().isBlank();
            case "category" -> c.getCategory() != null && !c.getCategory().isBlank();
            case "channel" -> c.getChannel() != null && !c.getChannel().isBlank();
            case "sku" -> c.getSku() != null && !c.getSku().isBlank();
            case "region" -> c.getRegion() != null && !c.getRegion().isBlank();
            case "minPurchaseCount" -> c.getMinPurchaseCount() != null;
            case "minSpend" -> c.getMinSpend() != null;
            case "windowDays" -> c.getWindowDays() != null;
            case "firstPurchaseOnly" -> Boolean.TRUE.equals(c.getFirstPurchaseOnly());
            default -> false;
        };
    }

    private static boolean hasAnyCriteriaSet(ReferralRuleCriteria c) {
        if (c == null) {
            return false;
        }
        return isCriteriaFieldSet(c, "merchantId")
            || isCriteriaFieldSet(c, "category")
            || isCriteriaFieldSet(c, "channel")
            || isCriteriaFieldSet(c, "sku")
            || isCriteriaFieldSet(c, "region")
            || isCriteriaFieldSet(c, "minPurchaseCount")
            || isCriteriaFieldSet(c, "minSpend")
            || isCriteriaFieldSet(c, "windowDays")
            || Boolean.TRUE.equals(c.getFirstPurchaseOnly())
            || (c.getEventTypes() != null && !c.getEventTypes().isEmpty())
            || (c.getMetadataFilters() != null && !c.getMetadataFilters().isEmpty());
    }

    private static String primaryEventType(ReferralMilestoneRule rule) {
        if (rule.getTrigger() == ReferralRuleTrigger.PURCHASE) {
            return "PURCHASE";
        }
        if (rule.getTrigger() == ReferralRuleTrigger.INTEGRATION_EVENT
            && rule.getCriteria().getEventTypes() != null
            && !rule.getCriteria().getEventTypes().isEmpty()) {
            return rule.getCriteria().getEventTypes().get(0).trim().toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private static void validateRules(ReferralProgrammeConfig config) {
        if (config.getMilestoneRules() == null) {
            throw new ReferralException("INVALID_CONFIG", "milestoneRules is required");
        }
        Set<String> keys = new HashSet<>();
        for (ReferralMilestoneRule rule : config.getMilestoneRules()) {
            if (rule == null || rule.getKey() == null || rule.getKey().isBlank()) {
                throw new ReferralException("INVALID_CONFIG", "Each milestone rule requires a key");
            }
            String key = rule.getKey().trim().toLowerCase();
            if (!ReferralMilestoneRuleRegistry.isValidKey(key)) {
                throw new ReferralException(
                    "INVALID_CONFIG",
                    "Rule key must be lowercase slug (a-z, 0-9, underscore): " + key
                );
            }
            if (!keys.add(key)) {
                throw new ReferralException("INVALID_CONFIG", "Duplicate milestone rule key: " + key);
            }
            if (rule.getLabel() == null || rule.getLabel().isBlank()) {
                throw new ReferralException("INVALID_CONFIG", "Rule " + key + " requires a label");
            }
            if (rule.getTrigger() == null) {
                throw new ReferralException("INVALID_CONFIG", "Rule " + key + " requires a trigger");
            }
            validateRuleCriteria(rule);
        }
    }

    private static void validateRuleCriteria(ReferralMilestoneRule rule) {
        var c = rule.getCriteria();
        if (rule.getTrigger() == ReferralRuleTrigger.LINK) {
            return;
        }
        if (rule.getTrigger() == ReferralRuleTrigger.INTEGRATION_EVENT) {
            if (c.getEventTypes() == null || c.getEventTypes().isEmpty()) {
                throw new ReferralException(
                    "INVALID_CONFIG",
                    "Rule " + rule.getKey() + " requires at least one integration event type"
                );
            }
            for (String et : c.getEventTypes()) {
                if (et == null || et.isBlank()) {
                    throw new ReferralException("INVALID_CONFIG", "eventTypes cannot be blank for rule " + rule.getKey());
                }
                if ("PURCHASE".equalsIgnoreCase(et.trim())) {
                    throw new ReferralException(
                        "INVALID_CONFIG",
                        "Use PURCHASE trigger for purchase rules, not eventTypes (rule " + rule.getKey() + ")"
                    );
                }
            }
            return;
        }
        if (c.getWindowDays() != null && c.getWindowDays() < 1) {
            throw new ReferralException("INVALID_CONFIG", "windowDays must be >= 1 for rule " + rule.getKey());
        }
        if (c.getMinPurchaseCount() != null && c.getMinPurchaseCount() < 1) {
            throw new ReferralException("INVALID_CONFIG", "minPurchaseCount must be >= 1 for rule " + rule.getKey());
        }
        if (c.getMinSpend() != null && c.getMinSpend().signum() < 0) {
            throw new ReferralException("INVALID_CONFIG", "minSpend cannot be negative for rule " + rule.getKey());
        }
    }

    private static void validatePartyReward(ReferralPartyRewardConfig reward, String field, int stage) {
        if (reward == null) {
            return;
        }
        if (reward.getType() == ReferralRewardType.VOUCHER) {
            if (reward.getCatalogRewardUid() == null || reward.getCatalogRewardUid().isBlank()) {
                throw new ReferralException(
                    "INVALID_CONFIG",
                    "Stage " + stage + " " + field + " voucher requires catalogRewardUid"
                );
            }
            validatePoints(reward.getVoucherPointsToRedeem(), field + ".voucherPointsToRedeem", stage);
            validatePoints(reward.getVoucherFaceValue(), field + ".voucherFaceValue", stage);
        } else {
            validatePoints(reward.getPoints(), field + ".points", stage);
        }
    }

    private static void validatePointsBudget(ReferralPointsBudget budget) {
        if (budget.getMaxPoints() == null || budget.getMaxPoints().signum() <= 0) {
            throw new ReferralException("INVALID_CONFIG", "pointsBudget.maxPoints must be positive");
        }
        if (budget.getPeriod() == ReferralPointsBudget.Period.ROLLING_DAY
            && budget.getWindowDays() != null
            && budget.getWindowDays() < 1) {
            throw new ReferralException("INVALID_CONFIG", "pointsBudget.windowDays must be >= 1");
        }
    }

    private static void validatePoints(BigDecimal points, String field, int stage) {
        if (points == null) {
            return;
        }
        if (points.signum() < 0) {
            throw new ReferralException("INVALID_CONFIG", "Stage " + stage + " " + field + " cannot be negative");
        }
    }
}
