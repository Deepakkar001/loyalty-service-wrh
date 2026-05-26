package com.loyaltyos.rewards.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.service.ProgrammeService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardCatalogServiceTest {

    @Mock
    private ProgrammeService programmeService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RewardCatalogService service;

    @Test
    void resolveRedemption_usesCatalogPointsWhenUidProvided() {
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson("""
            {
              "rewardCatalog": {
                "version": 1,
                "rewardTypes": [{ "typeCode": "VOUCHER", "label": "Voucher" }],
                "items": [{
                  "rewardUid": "free_coffee",
                  "name": "Free Coffee",
                  "rewardType": "VOUCHER",
                  "status": "ACTIVE",
                  "pointsCost": 500
                }]
              }
            }
            """);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        var resolution = service.resolveRedemption("t1", "default", "free_coffee", null);
        assertThat(resolution.isValid()).isTrue();
        assertThat(resolution.resolvedPoints()).isEqualByComparingTo("500");
        assertThat(resolution.catalogItem().name()).isEqualTo("Free Coffee");
    }

    @Test
    void resolveRedemption_rejectsMismatchedPoints() {
        ProgrammeConfig cfg = new ProgrammeConfig();
        cfg.setConfigJson("""
            {
              "rewardCatalog": {
                "version": 1,
                "items": [{
                  "rewardUid": "free_coffee",
                  "name": "Free Coffee",
                  "rewardType": "VOUCHER",
                  "status": "ACTIVE",
                  "pointsCost": 500
                }]
              }
            }
            """);
        when(programmeService.getActiveConfigOrNull("t1", "default")).thenReturn(cfg);

        var resolution = service.resolveRedemption("t1", "default", "free_coffee", new BigDecimal("100"));
        assertThat(resolution.isValid()).isFalse();
        assertThat(resolution.errors()).containsKey("pointsToRedeem");
    }
}
