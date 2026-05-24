package com.loyaltyos.rewards.exception;

import java.math.BigDecimal;

public class RewardInsufficientBalanceException extends RuntimeException {

    private final BigDecimal currentBalance;
    private final BigDecimal pointsRequested;

    public RewardInsufficientBalanceException(
        BigDecimal currentBalance,
        BigDecimal pointsRequested
    ) {
        super("Insufficient points balance for redemption");
        this.currentBalance = currentBalance;
        this.pointsRequested = pointsRequested;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getPointsRequested() {
        return pointsRequested;
    }
}
