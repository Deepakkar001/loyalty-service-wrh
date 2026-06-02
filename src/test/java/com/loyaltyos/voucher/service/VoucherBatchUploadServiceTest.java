package com.loyaltyos.voucher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherBatchUploadResponse;
import com.loyaltyos.voucher.entity.VoucherBatch;
import com.loyaltyos.voucher.enums.VoucherBatchStatus;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.repository.VoucherBatchRepository;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherBatchUploadServiceTest {

    @Mock
    private VoucherBatchRepository batchRepository;

    @Mock
    private VoucherInventoryRepository inventoryRepository;

    @Mock
    private VoucherCodeCryptoService cryptoService;

    @Mock
    private RewardCatalogService rewardCatalogService;

    @Mock
    private VoucherDenominationMappingService denominationMappingService;

    private VoucherBatchUploadService service;

    private VoucherProperties properties;

    @BeforeEach
    void setUp() {
        properties = new VoucherProperties();
        properties.setMaxRows(1000);
        properties.setMaxFileSizeMb(50);
        service = new VoucherBatchUploadService(
            batchRepository,
            inventoryRepository,
            cryptoService,
            rewardCatalogService,
            properties,
            new ObjectMapper(),
            denominationMappingService
        );
        lenient().when(denominationMappingService.faceValueIndex(anyString(), anyString()))
            .thenReturn(Collections.emptyMap());
    }

    @Test
    void uploadBatch_rejectsNonVoucherCatalogItem() {
        RewardCatalogItem item = new RewardCatalogItem(
            "coffee", "Coffee", "PHYSICAL", "ACTIVE", BigDecimal.TEN, 0, "", Map.of()
        );
        when(rewardCatalogService.findActiveItem("t1", "default", "coffee"))
            .thenReturn(Optional.of(item));

        MockMultipartFile file = csvFile("code\nX1\n");

        assertThatThrownBy(() ->
            service.uploadBatch("t1", "default", "coffee", file, null)
        ).isInstanceOf(VoucherCatalogException.class);
    }

    @Test
    void uploadBatch_importsValidRows() {
        RewardCatalogItem item = new RewardCatalogItem(
            "amazon_500", "Amazon", "VOUCHER", "ACTIVE", new BigDecimal("500"), 0, "", Map.of()
        );
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(item));
        when(batchRepository.findByTenantIdAndFileSha256(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(batchRepository.save(any(VoucherBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.existsByTenantIdAndCodeHash(anyString(), anyString())).thenReturn(false);
        when(cryptoService.encryptCode(anyString())).thenReturn("encrypted");

        String csv = """
            code,pin,face_value,currency,expires_at,partner_sku
            amzn-001,1234,500.00,INR,2030-12-31T23:59:59Z,AMZ-500
            amzn-002,,250.00,INR,2030-12-31T23:59:59Z,AMZ-250
            """;
        MockMultipartFile file = csvFile(csv);

        VoucherBatchUploadResponse response = service.uploadBatch(
            "t1", "default", "amazon_500", file, null
        );

        assertThat(response.getStatus()).isEqualTo(VoucherBatchStatus.COMPLETED.name());
        assertThat(response.getImportedCount()).isEqualTo(2);
        assertThat(response.getTotalRowsUploaded()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<com.loyaltyos.voucher.entity.VoucherInventory>> captor =
            ArgumentCaptor.forClass(java.util.List.class);
        verify(inventoryRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void uploadBatch_returnsExistingBatchForDuplicateFileHash() {
        RewardCatalogItem item = new RewardCatalogItem(
            "amazon_500", "Amazon", "VOUCHER", "ACTIVE", new BigDecimal("500"), 0, "", Map.of()
        );
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(item));

        VoucherBatch existing = new VoucherBatch();
        existing.setBatchUid("batch_existing");
        existing.setStatus(VoucherBatchStatus.COMPLETED);
        existing.setImportedCount(5);
        existing.setTotalRowsUploaded(5);
        when(batchRepository.findByTenantIdAndFileSha256(anyString(), anyString()))
            .thenReturn(Optional.of(existing));

        String csv = """
            code,pin,face_value,currency,expires_at,partner_sku
            X,,1.00,INR,2030-01-01T00:00:00Z,
            """;
        MockMultipartFile file = csvFile(csv);
        VoucherBatchUploadResponse response = service.uploadBatch(
            "t1", "default", "amazon_500", file, null
        );

        assertThat(response.getBatchUid()).isEqualTo("batch_existing");
        verify(inventoryRepository, org.mockito.Mockito.never()).saveAll(any());
    }

    @Test
    void uploadBatch_failsWhenStandardHeaderMissing() {
        RewardCatalogItem item = new RewardCatalogItem(
            "amazon_500", "Amazon", "VOUCHER", "ACTIVE", new BigDecimal("500"), 0, "", Map.of()
        );
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(item));
        when(batchRepository.findByTenantIdAndFileSha256(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(batchRepository.save(any(VoucherBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = csvFile("code\nONLY-CODE\n");

        assertThatThrownBy(() ->
            service.uploadBatch("t1", "default", "amazon_500", file, null)
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("face_value");
    }

    @Test
    void uploadBatch_skipsRowWhenMandatoryValueMissing() {
        RewardCatalogItem item = new RewardCatalogItem(
            "amazon_500", "Amazon", "VOUCHER", "ACTIVE", new BigDecimal("500"), 0, "", Map.of()
        );
        when(rewardCatalogService.findActiveItem("t1", "default", "amazon_500"))
            .thenReturn(Optional.of(item));
        when(batchRepository.findByTenantIdAndFileSha256(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(batchRepository.save(any(VoucherBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.existsByTenantIdAndCodeHash(anyString(), anyString())).thenReturn(false);
        when(cryptoService.encryptCode(anyString())).thenReturn("encrypted");

        String csv = """
            code,pin,face_value,currency,expires_at,partner_sku
            GOOD-001,1234,500.00,INR,2030-12-31T23:59:59Z,AMZ-500
            BAD-002,,,INR,2030-12-31T23:59:59Z,
            """;
        VoucherBatchUploadResponse response = service.uploadBatch(
            "t1", "default", "amazon_500", csvFile(csv), null
        );

        assertThat(response.getImportedCount()).isEqualTo(1);
        assertThat(response.getErrorCount()).isEqualTo(1);
    }

    private static MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "codes.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
