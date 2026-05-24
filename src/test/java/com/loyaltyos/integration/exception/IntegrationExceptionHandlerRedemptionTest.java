package com.loyaltyos.integration.exception;

import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.exception.RewardRedemptionLimitExceededException;
import com.loyaltyos.rewards.exception.RewardRedemptionValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationExceptionHandlerRedemptionTest {

    private final IntegrationExceptionHandler handler = new IntegrationExceptionHandler();

    @Test
    void insufficientBalance_mapsTo400() {
        var response = handler.handleInsufficientBalance(
            new RewardInsufficientBalanceException(new BigDecimal("10"), new BigDecimal("50"))
        );
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo("INSUFFICIENT_BALANCE");
    }

    @Test
    void redemptionLimitExceeded_mapsTo400() {
        var response = handler.handleRedemptionLimitExceeded(
            new RewardRedemptionLimitExceededException("limits", Map.of("pointsToRedeem", "Minimum redemption is 100 points"))
        );
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo("REDEMPTION_LIMIT_EXCEEDED");
    }

    @Test
    void redemptionValidation_mapsTo400WithDetails() {
        var response = handler.handleRedemptionValidation(
            new RewardRedemptionValidationException("bad", Map.of("pointsToRedeem", "min"))
        );
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_FAILED");
    }
}
