package com.loyaltyos.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntegrationResponseMapperTest {

    private final IntegrationResponseMapper mapper = new IntegrationResponseMapper();

    @Test
    void earnings_useRecordedPreviousBalance_andEventCustomerFinalPoints() {
        LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
        core.setSuccess(true);
        core.setTotalPointsAwarded(new BigDecimal("0"));
        core.setReferralPointsToOtherCustomers(new BigDecimal("200"));
        core.setReferralPointsAwarded(new BigDecimal("200"));
        core.setPreviousBalance(new BigDecimal("50"));
        core.setNewBalance(new BigDecimal("50"));

        IntegrationParsedEvent request = new IntegrationParsedEvent(
            "PURCHASE",
            "evt-1",
            "default",
            "REFERRAL",
            "referee-002",
            BigDecimal.valueOf(500),
            null,
            Map.of("timestamp", "2026-06-03T10:00:00Z"),
            null,
            Map.of()
        );

        EventProcessingResponse response = mapper.toSuccessResponse(request, core, null, 10);

        assertThat(response.getEarnings().getFinalPoints()).isEqualByComparingTo("0");
        assertThat(response.getEarnings().getPreviousBalance()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getNewBalance()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getReferralPointsToOtherCustomers()).isEqualByComparingTo("200");
    }

    @Test
    void earnings_whenEventCustomerEarnsReferralPoints_balanceMathIsConsistent() {
        LoyaltyEventProcessResponse core = new LoyaltyEventProcessResponse();
        core.setSuccess(true);
        core.setTotalPointsAwarded(new BigDecimal("50"));
        core.setReferralPointsToOtherCustomers(new BigDecimal("200"));
        core.setReferralPointsAwarded(new BigDecimal("250"));
        core.setPreviousBalance(new BigDecimal("50"));
        core.setNewBalance(new BigDecimal("100"));

        IntegrationParsedEvent request = new IntegrationParsedEvent(
            "PURCHASE",
            "evt-2",
            "default",
            "REFERRAL",
            "referee-002",
            BigDecimal.valueOf(500),
            null,
            Map.of("timestamp", "2026-06-03T10:00:00Z"),
            null,
            Map.of()
        );

        EventProcessingResponse response = mapper.toSuccessResponse(request, core, null, 5);

        assertThat(response.getEarnings().getFinalPoints()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getPreviousBalance()).isEqualByComparingTo("50");
        assertThat(response.getEarnings().getNewBalance()).isEqualByComparingTo("100");
        assertThat(response.getEarnings().getReferralPointsToOtherCustomers()).isEqualByComparingTo("200");
    }
}
