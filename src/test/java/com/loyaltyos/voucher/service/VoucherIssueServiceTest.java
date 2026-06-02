package com.loyaltyos.voucher.service;

import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.rewards.dto.RedemptionRequest;
import com.loyaltyos.rewards.dto.RedemptionResult;
import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import com.loyaltyos.rewards.service.RewardRedemptionService;
import com.loyaltyos.rules.entity.PointsLedger;
import com.loyaltyos.rules.enums.LedgerEntryType;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import com.loyaltyos.voucher.entity.VoucherInventory;
import com.loyaltyos.voucher.enums.VoucherStatus;
import com.loyaltyos.voucher.exception.VoucherOutOfStockException;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherIssueServiceTest {

    @Mock
    private VoucherInventoryRepository inventoryRepository;

    @Mock
    private VoucherCodeCryptoService cryptoService;

    @Mock
    private RewardRedemptionService redemptionService;

    @Mock
    private RewardCatalogService rewardCatalogService;

    @Mock
    private PointsLedgerRepository pointsLedgerRepository;

    @Mock
    private VoucherAuditService auditService;

    @Mock
    private VoucherDenominationMappingService denominationMappingService;

    @InjectMocks
    private VoucherIssueService service;

    private RewardCatalogItem voucherItem;

    @BeforeEach
    void setUp() {
        when(denominationMappingService.hasActiveMappings(anyString(), anyString())).thenReturn(false);
        voucherItem = new RewardCatalogItem(
            "amazon_500",
            "Amazon 500",
            "VOUCHER",
            "ACTIVE",
            new BigDecimal("500"),
            0,
            "",
            Map.of()
        );
    }

    @Test
    void issueVoucher_idempotentWhenInventoryAlreadyIssued() {
        VoucherInventory issued = issuedInventory("red_1", "cust_1");
        when(inventoryRepository.findByRedemptionId("red_1")).thenReturn(Optional.of(issued));
        when(cryptoService.decryptCode("cipher")).thenReturn("AMZN-CODE");
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(voucherItem));

        VoucherIssueResponse response = service.issueVoucher(
            "t1", "default", "amazon_500", "cust_1", "red_1"
        );

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.isIdempotentReplay()).isTrue();
        assertThat(response.getVoucher().getCode()).isEqualTo("AMZN-CODE");
        verify(redemptionService, never()).redeem(any(), any());
        verify(inventoryRepository, never()).lockNextAvailableId(any(), any(), any());
    }

    @Test
    void issueVoucher_outOfStockWhenNoRowLocked() {
        when(inventoryRepository.findByRedemptionId("red_2")).thenReturn(Optional.empty());
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_2"
        )).thenReturn(Optional.empty());
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(voucherItem));
        when(inventoryRepository.lockNextAvailableId("t1", "default", "amazon_500"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.issueVoucher("t1", "default", "amazon_500", "cust_1", "red_2")
        ).isInstanceOf(VoucherOutOfStockException.class);

        verify(redemptionService, never()).redeem(any(), any());
    }

    @Test
    void issueVoucher_insufficientBalanceReleasesInventory() {
        when(inventoryRepository.findByRedemptionId("red_3")).thenReturn(Optional.empty());
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_3"
        )).thenReturn(Optional.empty());
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(voucherItem));
        when(inventoryRepository.lockNextAvailableId("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(42L));

        VoucherInventory row = availableInventory(42L);
        when(inventoryRepository.findById(42L)).thenReturn(Optional.of(row));
        when(redemptionService.redeem(eq("t1"), any(RedemptionRequest.class)))
            .thenThrow(new RewardInsufficientBalanceException(new BigDecimal("100"), new BigDecimal("500")));

        assertThatThrownBy(() -> service.issueVoucher(
            "t1", "default", "amazon_500", "cust_1", "red_3"
        )).isInstanceOf(RewardInsufficientBalanceException.class);

        assertThat(row.getStatus()).isEqualTo(VoucherStatus.AVAILABLE);
        assertThat(row.getRedemptionId()).isNull();
        verify(inventoryRepository).save(row);
    }

    @Test
    void issueVoucher_happyPathDebitsPointsAndReturnsCode() {
        when(inventoryRepository.findByRedemptionId("red_4")).thenReturn(Optional.empty());
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_4"
        )).thenReturn(Optional.empty());
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(voucherItem));
        when(inventoryRepository.lockNextAvailableId("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(7L));

        VoucherInventory row = availableInventory(7L);
        when(inventoryRepository.findById(7L)).thenReturn(Optional.of(row));

        RedemptionResult redemption = new RedemptionResult();
        redemption.setLedgerId(99L);
        redemption.setNewBalance(new BigDecimal("1500"));
        redemption.setIdempotentReplay(false);
        when(redemptionService.redeem(eq("t1"), any(RedemptionRequest.class))).thenReturn(redemption);

        when(cryptoService.decryptCode("cipher")).thenReturn("AMZN-NEW");

        VoucherIssueResponse response = service.issueVoucher(
            "t1", "default", "amazon_500", "cust_1", "red_4"
        );

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getVoucher().getCode()).isEqualTo("AMZN-NEW");
        assertThat(response.getLedgerId()).isEqualTo(99L);
        assertThat(response.getNewBalance()).isEqualByComparingTo("1500");
        assertThat(row.getStatus()).isEqualTo(VoucherStatus.ISSUED);
        assertThat(row.getLedgerId()).isEqualTo(99L);

        ArgumentCaptor<RedemptionRequest> captor = ArgumentCaptor.forClass(RedemptionRequest.class);
        verify(redemptionService).redeem(eq("t1"), captor.capture());
        assertThat(captor.getValue().getRedemptionId()).isEqualTo("red_4");
        assertThat(captor.getValue().getCatalogRewardUid()).isEqualTo("amazon_500");
        assertThat(captor.getValue().getChannel()).isEqualTo("VOUCHER_ISSUANCE");
    }

    @Test
    void issueVoucher_sameRedemptionIdDoesNotCallRedeemTwiceOnReplay() {
        VoucherInventory issued = issuedInventory("red_5", "cust_1");
        when(inventoryRepository.findByRedemptionId("red_5")).thenReturn(Optional.of(issued));
        when(cryptoService.decryptCode("cipher")).thenReturn("SAME-CODE");
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(voucherItem));

        service.issueVoucher("t1", "default", "amazon_500", "cust_1", "red_5");
        service.issueVoucher("t1", "default", "amazon_500", "cust_1", "red_5");

        verify(redemptionService, never()).redeem(any(), any());
    }

    @Test
    void issueVoucher_ledgerReplayWithoutInventoryReturnsError() {
        PointsLedger ledger = new PointsLedger();
        ledger.setId(55L);
        ledger.setPoints(new BigDecimal("500"));
        ledger.setEntryType(LedgerEntryType.DEBIT);
        ledger.setCreatedAt(Instant.now());

        when(inventoryRepository.findByRedemptionId("red_6")).thenReturn(Optional.empty());
        when(pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            "t1", "cust_1", "redeem:red_6"
        )).thenReturn(Optional.of(ledger));
        when(inventoryRepository.findByRedemptionId("red_6")).thenReturn(Optional.empty());

        VoucherIssueResponse response = service.issueVoucher(
            "t1", "default", "amazon_500", "cust_1", "red_6"
        );

        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.isIdempotentReplay()).isTrue();
        verify(inventoryRepository, never()).lockNextAvailableId(any(), any(), any());
    }

    private static VoucherInventory availableInventory(long id) {
        VoucherInventory inv = new VoucherInventory();
        inv.setId(id);
        inv.setInventoryUid("inv_" + id);
        inv.setCodeCiphertext("cipher");
        inv.setStatus(VoucherStatus.AVAILABLE);
        inv.setTenantId("t1");
        inv.setProgrammeUid("default");
        inv.setCatalogRewardUid("amazon_500");
        inv.setBatchUid("batch_1");
        inv.setCodeHash("hash");
        return inv;
    }

    private static VoucherInventory issuedInventory(String redemptionId, String customerId) {
        VoucherInventory inv = availableInventory(1L);
        inv.setStatus(VoucherStatus.ISSUED);
        inv.setRedemptionId(redemptionId);
        inv.setCustomerId(customerId);
        inv.setLedgerId(10L);
        inv.setIssuedAt(Instant.now());
        return inv;
    }
}
