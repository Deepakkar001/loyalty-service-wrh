package com.loyaltyos.rewards.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.loyaltyos.voucher.repository.VoucherDenominationMappingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class RewardCatalogServiceTest {

    @Mock
    private RewardCatalogDbMergeService catalogDbMergeService;

    @Mock
    private ObjectProvider<VoucherDenominationMappingRepository> denominationMappingRepository;

    @InjectMocks
    private RewardCatalogService service;

    @Test
    void resolveRedemption_usesCatalogPointsWhenUidProvided() {
        RewardCatalogItem item = new RewardCatalogItem(
            "free_coffee",
            "Free Coffee",
            "VOUCHER",
            "ACTIVE",
            new BigDecimal("500"),
            0,
            "",
            Map.of()
        );
        when(catalogDbMergeService.mergeFromDatabase("t1", "default"))
            .thenReturn(new RewardCatalogDbMergeService.MergedRewardCatalogResult(
                new RewardCatalogSnapshot(1, List.of(), List.of(item)),
                1,
                List.of(1),
                List.of(),
                0,
                null
            ));

        var resolution = service.resolveRedemption("t1", "default", "free_coffee", null);
        assertThat(resolution.isValid()).isTrue();
        assertThat(resolution.resolvedPoints()).isEqualByComparingTo("500");
        assertThat(resolution.catalogItem().name()).isEqualTo("Free Coffee");
    }

    @Test
    void resolveRedemption_rejectsMismatchedPoints() {
        RewardCatalogItem item = new RewardCatalogItem(
            "free_coffee",
            "Free Coffee",
            "VOUCHER",
            "ACTIVE",
            new BigDecimal("500"),
            0,
            "",
            Map.of()
        );
        when(catalogDbMergeService.mergeFromDatabase("t1", "default"))
            .thenReturn(new RewardCatalogDbMergeService.MergedRewardCatalogResult(
                new RewardCatalogSnapshot(1, List.of(), List.of(item)),
                1,
                List.of(1),
                List.of(),
                0,
                null
            ));

        var resolution = service.resolveRedemption("t1", "default", "free_coffee", new BigDecimal("100"));
        assertThat(resolution.isValid()).isFalse();
    }
}
