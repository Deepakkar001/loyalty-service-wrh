package com.loyaltyos.integration.service;

import com.loyaltyos.integration.dto.IntegrationBalanceDetailResponse;
import com.loyaltyos.integration.dto.IntegrationBalanceResponse;
import com.loyaltyos.rewards.dto.LedgerTransactionDto;
import com.loyaltyos.rewards.dto.RewardBalanceDetailResponse;
import com.loyaltyos.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.service.PointsLedgerQueryService;
import com.loyaltyos.rewards.service.RewardBalanceQueryService;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationBalanceServiceTest {

    @Mock
    private ProgrammeService programmeService;

    @Mock
    private RewardIssuanceService rewardIssuanceService;

    @Mock
    private RewardBalanceQueryService rewardBalanceQueryService;

    @Mock
    private PointsLedgerQueryService pointsLedgerQueryService;

    @InjectMocks
    private IntegrationBalanceService service;

    @Test
    void getBalance_mapsCoreResponse() {
        RewardBalanceResponse core = new RewardBalanceResponse();
        core.setTenantId("t1");
        core.setProgrammeUid("default");
        core.setCustomerId("c1");
        core.setBalance(new BigDecimal("100"));
        core.setLedgerDerivedBalance(new BigDecimal("100"));
        core.setUpdatedAt(Instant.parse("2026-05-01T00:00:00Z"));
        when(rewardIssuanceService.getBalance("t1", "default", "c1")).thenReturn(core);

        IntegrationBalanceResponse out = service.getBalance("t1", "default", "c1");
        assertThat(out.getBalance()).isEqualByComparingTo("100");
        assertThat(out.getCustomerId()).isEqualTo("c1");
    }

    @Test
    void getBalanceDetail_mapsCachedAndVariance() {
        RewardBalanceDetailResponse core = new RewardBalanceDetailResponse();
        core.setTenantId("t1");
        core.setCachedBalance(new BigDecimal("90"));
        core.setLedgerDerivedBalance(new BigDecimal("100"));
        core.setVariance(new BigDecimal("10"));
        core.setExpiringWithin7Days(new BigDecimal("5"));
        when(rewardBalanceQueryService.getBalanceDetail("t1", "default", "c1")).thenReturn(core);

        IntegrationBalanceDetailResponse out = service.getBalanceDetail("t1", "default", "c1");
        assertThat(out.getBalance()).isEqualByComparingTo("90");
        assertThat(out.getVariance()).isEqualByComparingTo("10");
        assertThat(out.getExpiringWithin7Days()).isEqualByComparingTo("5");
    }

    @Test
    void listTransactions_delegatesToLedgerQuery() {
        LedgerTransactionDto row = new LedgerTransactionDto();
        row.setLedgerId(1L);
        row.setPoints(BigDecimal.ONE);
        when(pointsLedgerQueryService.listCustomerTransactions(
            "t1", "default", "c1", null, null, null, PageRequest.of(0, 10)
        )).thenReturn(new PageImpl<>(List.of(row)));

        assertThat(service.listTransactions(
            "t1", "default", "c1", null, null, null, PageRequest.of(0, 10)
        ).getTotalElements()).isOne();
    }
}
