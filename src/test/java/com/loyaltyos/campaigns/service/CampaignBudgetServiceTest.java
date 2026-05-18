package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.model.BudgetDecrementResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignBudgetServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CampaignBudgetAlertNotifier alertNotifier;

    private CampaignBudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new CampaignBudgetService(jdbcTemplate, alertNotifier);
    }

    @Test
    void tryDecrementBudget_returnsFailedWhenNoRowsUpdated() {
        when(jdbcTemplate.update(any(String.class), any(), any(), any(), any())).thenReturn(0);

        BudgetDecrementResult result = budgetService.tryDecrementBudget("tenant-1", "camp-1", new BigDecimal("50"));

        assertFalse(result.success());
        assertTrue(result.budgetExhausted());
        verify(jdbcTemplate, never()).query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(), any());
    }

    @Test
    void tryDecrementBudget_skipsJdbcWhenCostZero() {
        BudgetDecrementResult result = budgetService.tryDecrementBudget("tenant-1", "camp-1", BigDecimal.ZERO);

        assertTrue(result.success());
        verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any());
    }
}
