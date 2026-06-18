package com.loyaltyos.merchants.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.merchants.config.MerchantProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MerchantValidationServiceTest {

    @Mock
    private MerchantProperties merchantProperties;

    private MerchantValidationService validationService;

    @BeforeEach
    void setUp() {
        when(merchantProperties.getMinEarnRateMultiplier()).thenReturn(new BigDecimal("0.5"));
        when(merchantProperties.getMaxEarnRateMultiplier()).thenReturn(new BigDecimal("10"));
        when(merchantProperties.getCommissionMaxRate()).thenReturn(new BigDecimal("10"));
        when(merchantProperties.getMaxMerchantCampaignBudget()).thenReturn(new BigDecimal("500000"));
        validationService = new MerchantValidationService(merchantProperties, new ObjectMapper());
    }

    @Test
    void validateEarnRate_withinBounds() {
        assertDoesNotThrow(() -> validationService.validateEarnRateMultiplier(new BigDecimal("2.5")));
    }

    @Test
    void validateEarnRate_rejectsOutOfBounds() {
        assertThrows(
            ResponseStatusException.class,
            () -> validationService.validateEarnRateMultiplier(new BigDecimal("20"))
        );
    }

    @Test
    void validateCommission_acceptsValidJson() {
        assertDoesNotThrow(
            () -> validationService.validateCommissionConfigJson("{\"type\":\"PERCENT\",\"rate\":2.5}")
        );
    }

    @Test
    void validateCommission_rejectsInvalidType() {
        assertThrows(
            ResponseStatusException.class,
            () -> validationService.validateCommissionConfigJson("{\"type\":\"INVALID\",\"rate\":1}")
        );
    }

    @Test
    void validateMerchantCampaignBudget_rejectsOverMax() {
        assertThrows(
            ResponseStatusException.class,
            () -> validationService.validateMerchantCampaignBudget(new BigDecimal("600000"))
        );
    }
}
