package com.loyaltyos.onboarding.rules.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ProgrammeEvaluationContext {

    private final BigDecimal basePointsRate;
    private final BigDecimal pointsMonetaryValue;
    private final BigDecimal dailyCap;
    private final BigDecimal monthlyCap;
    private final String conflictDefaultStrategy;
    private final boolean allowRuleOverride;
    private final BigDecimal resolvedTierMultiplier;

    /**
     * First-level property names allowed under {@code event.*} in conditions (from programme eventSchema + core).
     * When empty, any syntactically valid {@code event.*} path is allowed (legacy / missing schema).
     */
    private final Set<String> allowedEventPropertyNames;

    public ProgrammeEvaluationContext(
        BigDecimal basePointsRate,
        BigDecimal pointsMonetaryValue,
        BigDecimal dailyCap,
        BigDecimal monthlyCap,
        String conflictDefaultStrategy,
        boolean allowRuleOverride,
        BigDecimal resolvedTierMultiplier,
        Set<String> allowedEventPropertyNames
    ) {
        this.basePointsRate = basePointsRate;
        this.pointsMonetaryValue = pointsMonetaryValue;
        this.dailyCap = dailyCap;
        this.monthlyCap = monthlyCap;
        this.conflictDefaultStrategy = conflictDefaultStrategy;
        this.allowRuleOverride = allowRuleOverride;
        this.resolvedTierMultiplier = resolvedTierMultiplier;
        // Preserve Lombok @Builder.Default semantics
        this.allowedEventPropertyNames = allowedEventPropertyNames != null ? allowedEventPropertyNames : Collections.emptySet();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private BigDecimal basePointsRate;
        private BigDecimal pointsMonetaryValue;
        private BigDecimal dailyCap;
        private BigDecimal monthlyCap;
        private String conflictDefaultStrategy;
        private boolean allowRuleOverride;
        private BigDecimal resolvedTierMultiplier;
        private Set<String> allowedEventPropertyNames = Collections.emptySet();

        private Builder() {}

        public Builder basePointsRate(BigDecimal basePointsRate) { this.basePointsRate = basePointsRate; return this; }
        public Builder pointsMonetaryValue(BigDecimal pointsMonetaryValue) { this.pointsMonetaryValue = pointsMonetaryValue; return this; }
        public Builder dailyCap(BigDecimal dailyCap) { this.dailyCap = dailyCap; return this; }
        public Builder monthlyCap(BigDecimal monthlyCap) { this.monthlyCap = monthlyCap; return this; }
        public Builder conflictDefaultStrategy(String conflictDefaultStrategy) { this.conflictDefaultStrategy = conflictDefaultStrategy; return this; }
        public Builder allowRuleOverride(boolean allowRuleOverride) { this.allowRuleOverride = allowRuleOverride; return this; }
        public Builder resolvedTierMultiplier(BigDecimal resolvedTierMultiplier) { this.resolvedTierMultiplier = resolvedTierMultiplier; return this; }
        public Builder allowedEventPropertyNames(Set<String> allowedEventPropertyNames) {
            this.allowedEventPropertyNames = allowedEventPropertyNames != null ? allowedEventPropertyNames : Collections.emptySet();
            return this;
        }

        public ProgrammeEvaluationContext build() {
            return new ProgrammeEvaluationContext(
                basePointsRate,
                pointsMonetaryValue,
                dailyCap,
                monthlyCap,
                conflictDefaultStrategy,
                allowRuleOverride,
                resolvedTierMultiplier,
                allowedEventPropertyNames
            );
        }
    }

    public Map<String, Object> asTenantVariableMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("basePointsRate", basePointsRate != null ? basePointsRate : BigDecimal.ZERO);
        m.put("pointsMonetaryValue", pointsMonetaryValue != null ? pointsMonetaryValue : BigDecimal.ZERO);
        m.put("dailyCap", dailyCap);
        m.put("monthlyCap", monthlyCap);
        m.put("conflictDefaultStrategy", conflictDefaultStrategy != null ? conflictDefaultStrategy : "BEST_FOR_CUSTOMER");
        m.put("allowRuleOverride", allowRuleOverride);
        return m;
    }

    public BigDecimal getBasePointsRate() { return basePointsRate; }
    public BigDecimal getPointsMonetaryValue() { return pointsMonetaryValue; }
    public BigDecimal getDailyCap() { return dailyCap; }
    public BigDecimal getMonthlyCap() { return monthlyCap; }
    public String getConflictDefaultStrategy() { return conflictDefaultStrategy; }
    public boolean isAllowRuleOverride() { return allowRuleOverride; }
    public BigDecimal getResolvedTierMultiplier() { return resolvedTierMultiplier; }
    public Set<String> getAllowedEventPropertyNames() { return allowedEventPropertyNames; }
}
