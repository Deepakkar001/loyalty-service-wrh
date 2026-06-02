package com.loyaltyos.integration.service;

import com.loyaltyos.integration.dto.IntegrationRedemptionRequest;
import com.loyaltyos.integration.dto.IntegrationRedemptionResponse;
import com.loyaltyos.integration.dto.IntegrationRedemptionValidationResponse;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.dto.RedemptionResult;
import com.loyaltyos.rewards.dto.RedemptionValidationResult;
import com.loyaltyos.rewards.service.RewardRedemptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationRedemptionServiceTest {

    @Mock
    private ProgrammeService programmeService;

    @Mock
    private RewardRedemptionService rewardRedemptionService;

    @InjectMocks
    private IntegrationRedemptionService service;

    @Test
    void validate_mapsValidationResult() {
        RedemptionValidationResult core = new RedemptionValidationResult();
        core.setStatus("VALIDATION_SUCCESS");
        core.setRedemptionId("red_1");
        core.setValid(true);
        core.setCurrentBalance(new BigDecimal("100"));
        core.setPointsToRedeem(new BigDecimal("50"));
        core.setTimestamp(Instant.parse("2026-05-01T00:00:00Z"));
        when(rewardRedemptionService.validateRedemption(eq("t1"), any())).thenReturn(core);

        IntegrationRedemptionRequest req = new IntegrationRedemptionRequest();
        req.setRedemptionId("red_1");
        req.setCustomerId("c1");
        req.setPointsToRedeem(new BigDecimal("50"));

        IntegrationRedemptionValidationResponse out = service.validate("t1", req);
        assertThat(out.isValid()).isTrue();
        assertThat(out.getCurrentBalance()).isEqualByComparingTo("100");
    }

    @Test
    void redeem_mapsRedemptionResult() {
        RedemptionResult core = new RedemptionResult();
        core.setStatus("SUCCESS");
        core.setRedemptionId("red_1");
        core.setCustomerId("c1");
        core.setProgrammeUid("default");
        core.setPointsRedeemed(new BigDecimal("50"));
        core.setNewBalance(new BigDecimal("50"));
        core.setLedgerId(9L);
        core.setIdempotentReplay(false);
        core.setTimestamp(Instant.parse("2026-05-01T00:00:00Z"));
        when(rewardRedemptionService.redeem(eq("t1"), any())).thenReturn(core);

        IntegrationRedemptionRequest req = new IntegrationRedemptionRequest();
        req.setRedemptionId("red_1");
        req.setCustomerId("c1");
        req.setPointsToRedeem(new BigDecimal("50"));

        IntegrationRedemptionResponse out = service.redeem("t1", req);
        assertThat(out.getStatus()).isEqualTo("SUCCESS");
        assertThat(out.getLedgerId()).isEqualTo(9L);
        assertThat(out.getNewBalance()).isEqualByComparingTo("50");
    }
}
