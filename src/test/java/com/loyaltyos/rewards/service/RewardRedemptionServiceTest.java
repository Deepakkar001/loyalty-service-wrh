package com.loyaltyos.rewards.service;

import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.rewards.dto.RedemptionLimits;
import com.loyaltyos.rewards.dto.RedemptionRequest;
import com.loyaltyos.rewards.dto.RedemptionResult;
import com.loyaltyos.rewards.dto.RedemptionValidationResult;
import com.loyaltyos.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.exception.RewardRedemptionLimitExceededException;
import com.loyaltyos.rewards.exception.RewardRedemptionValidationException;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import com.loyaltyos.rules.entity.PointsLedger;
import com.loyaltyos.rules.enums.LedgerEntryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardRedemptionServiceTest {

    @Mock
    private RewardIssuanceService rewardIssuanceService;

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @Mock
    private CustomerBalanceCacheSyncService customerBalanceCacheSyncService;

    @Mock
    private ProgrammeRedemptionConfigResolver redemptionConfigResolver;

    @Mock
    private RewardCatalogService rewardCatalogService;

    @InjectMocks
    private RewardRedemptionService service;

    private RedemptionRequest request;

    @BeforeEach
    void setUp() {
        request = new RedemptionRequest();
        request.setRedemptionId("red_1");
        request.setCustomerId("cust_1");
        request.setProgrammeUid("default");
        request.setPointsToRedeem(new BigDecimal("50"));
        lenient().when(redemptionConfigResolver.resolve("t1", "default")).thenReturn(RedemptionLimits.none());
        lenient().when(rewardCatalogService.resolveRedemption(eq("t1"), eq("default"), any(), any()))
            .thenAnswer(inv -> {
                String catalogUid = inv.getArgument(2);
                BigDecimal pts = inv.getArgument(3);
                if (catalogUid != null && !String.valueOf(catalogUid).isBlank()) {
                    return new RewardCatalogService.CatalogRedemptionResolution(null, pts, java.util.Map.of());
                }
                return new RewardCatalogService.CatalogRedemptionResolution(null, pts, java.util.Map.of());
            });
    }

    private RewardBalanceResponse balance(BigDecimal amount) {
        RewardBalanceResponse b = new RewardBalanceResponse();
        b.setBalance(amount);
        return b;
    }

    @Test
    void validateRedemption_successWhenBalanceSufficient() {
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("100")));

        RedemptionValidationResult result = service.validateRedemption("t1", request);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getStatus()).isEqualTo("VALIDATION_SUCCESS");
    }

    @Test
    void validateRedemption_failsWhenInsufficientBalance() {
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("10")));

        RedemptionValidationResult result = service.validateRedemption("t1", request);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFieldErrors()).containsKey("pointsToRedeem");
    }

    @Test
    void validateRedemption_enforcesMinPoints() {
        when(redemptionConfigResolver.resolve("t1", "default"))
            .thenReturn(new RedemptionLimits(new BigDecimal("100"), null));
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("500")));

        RedemptionValidationResult result = service.validateRedemption("t1", request);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFieldErrors().get("pointsToRedeem")).contains("Minimum redemption");
    }

    @Test
    void redeem_writesDebitAndUpdatesCache() {
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_1")).thenReturn(Optional.empty());
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("100")), balance(new BigDecimal("50")));

        PointsLedger saved = PointsLedger.builder().id(99L).entryType(LedgerEntryType.DEBIT).points(new BigDecimal("50")).build();
        when(pointsLedgerRepository.save(any(PointsLedger.class))).thenReturn(saved);

        RedemptionResult result = service.redeem("t1", request);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getPointsRedeemed()).isEqualByComparingTo("50");
        assertThat(result.getLedgerId()).isEqualTo(99L);
        assertThat(result.isIdempotentReplay()).isFalse();

        ArgumentCaptor<PointsLedger> captor = ArgumentCaptor.forClass(PointsLedger.class);
        verify(pointsLedgerRepository).save(captor.capture());
        assertThat(captor.getValue().getEntryType()).isEqualTo(LedgerEntryType.DEBIT);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("redeem:red_1");
        verify(customerBalanceCacheSyncService).decrementOrRealignToLedger(
            eq("t1"),
            eq("default"),
            eq("cust_1"),
            argThat(b -> b != null && b.compareTo(new BigDecimal("50")) == 0)
        );
    }

    @Test
    void redeem_idempotentReplaySkipsWrite() {
        PointsLedger existing = PointsLedger.builder()
            .id(7L)
            .entryType(LedgerEntryType.DEBIT)
            .points(new BigDecimal("50"))
            .build();
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_1")).thenReturn(Optional.of(existing));
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("50")));

        RedemptionResult result = service.redeem("t1", request);
        assertThat(result.isIdempotentReplay()).isTrue();
        verify(pointsLedgerRepository, never()).save(any());
    }

    @Test
    void redeem_throwsInsufficientBalance() {
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_1")).thenReturn(Optional.empty());
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("10")));

        assertThatThrownBy(() -> service.redeem("t1", request))
            .isInstanceOf(RewardInsufficientBalanceException.class);
    }

    @Test
    void redeem_throwsLimitExceededForMinPoints() {
        when(redemptionConfigResolver.resolve("t1", "default"))
            .thenReturn(new RedemptionLimits(new BigDecimal("100"), null));
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_1")).thenReturn(Optional.empty());
        when(rewardIssuanceService.getBalance("t1", "default", "cust_1"))
            .thenReturn(balance(new BigDecimal("500")));

        assertThatThrownBy(() -> service.redeem("t1", request))
            .isInstanceOf(RewardRedemptionLimitExceededException.class);
    }
}
