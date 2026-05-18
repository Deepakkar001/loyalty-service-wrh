package com.loyaltyos.campaigns.model;

import java.math.BigDecimal;

public record BudgetDecrementResult(
    boolean success,
    boolean budgetExhausted,
    boolean alertThresholdCrossed,
    BigDecimal budgetConsumed,
    BigDecimal budgetTotal
) {
    public static BudgetDecrementResult failed() {
        return new BudgetDecrementResult(false, true, false, null, null);
    }
}
