package com.loyaltyos.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.dto.IntegrationBalanceDetailResponse;
import com.loyaltyos.integration.dto.IntegrationBalanceResponse;
import com.loyaltyos.integration.dto.IntegrationRedemptionRequest;
import com.loyaltyos.integration.dto.IntegrationRedemptionResponse;
import com.loyaltyos.integration.dto.IntegrationRedemptionValidationResponse;
import com.loyaltyos.integration.dto.IntegrationTransactionResponse;
import com.loyaltyos.integration.exception.IntegrationExceptionHandler;
import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationBalanceService;
import com.loyaltyos.integration.service.IntegrationMetricsService;
import com.loyaltyos.integration.service.IntegrationRedemptionService;
import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.exception.RewardRedemptionLimitExceededException;
import com.loyaltyos.rewards.exception.RewardRedemptionValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.bind.support.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IntegrationRewardsController.class)
@Import({
    IntegrationRewardsController.class,
    IntegrationExceptionHandler.class,
    IntegrationRewardsControllerMvcTest.MvcSecurityConfig.class
})
@AutoConfigureMockMvc(addFilters = false)
class IntegrationRewardsControllerMvcTest {

    static class MvcSecurityConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IntegrationBalanceService balanceService;

    @MockBean
    private IntegrationRedemptionService redemptionService;

    @MockBean
    private IntegrationAuditService auditService;

    @MockBean
    private IntegrationMetricsService metricsService;

    @BeforeEach
    void stubSideEffects() {
        lenient().doNothing().when(auditService).logApiRequest(
            anyString(), anyString(), anyString(), anyString(), anyString(),
            any(), any(), anyInt(), anyInt(), any(), any(), anyString(), anyString(), anyString()
        );
        lenient().doNothing().when(metricsService).recordRequest(anyString(), anyString(), anyInt(), anyLong());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private static RequestPostProcessor apiKeyAuth(String tenantId) {
        return request -> {
            ApiKeyPrincipal principal = new ApiKeyPrincipal(tenantId, "key_uid", ApiKeyEnvironment.SANDBOX);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            SecurityContextHolder.setContext(context);
            request.setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            );
            return request;
        };
    }

    @Test
    void getBalance_returns200() throws Exception {
        IntegrationBalanceResponse body = new IntegrationBalanceResponse();
        body.setTenantId("t1");
        body.setCustomerId("c1");
        body.setBalance(new BigDecimal("100"));
        when(balanceService.getBalance("t1", "default", "c1")).thenReturn(body);

        mockMvc.perform(get("/api/v1/integration/t1/customers/c1/balance")
                .with(apiKeyAuth("t1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void getBalanceDetail_returns200() throws Exception {
        IntegrationBalanceDetailResponse body = new IntegrationBalanceDetailResponse();
        body.setBalance(new BigDecimal("90"));
        body.setVariance(new BigDecimal("10"));
        when(balanceService.getBalanceDetail("t1", "default", "c1")).thenReturn(body);

        mockMvc.perform(get("/api/v1/integration/t1/customers/c1/balance-detail")
                .with(apiKeyAuth("t1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(90));
    }

    @Test
    void listTransactions_returnsEmptyPage() throws Exception {
        when(balanceService.listTransactions(eq("t1"), eq("default"), eq("c1"), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/integration/t1/customers/c1/transactions")
                .with(apiKeyAuth("t1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void validateRedemption_returns200() throws Exception {
        IntegrationRedemptionValidationResponse body = new IntegrationRedemptionValidationResponse();
        body.setValid(true);
        body.setStatus("VALIDATION_SUCCESS");
        when(redemptionService.validate(eq("t1"), any())).thenReturn(body);

        IntegrationRedemptionRequest req = redemptionRequest();

        mockMvc.perform(post("/api/v1/integration/t1/redemptions/validate")
                .with(apiKeyAuth("t1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void redeem_returns200() throws Exception {
        IntegrationRedemptionResponse body = new IntegrationRedemptionResponse();
        body.setStatus("SUCCESS");
        body.setPointsRedeemed(new BigDecimal("50"));
        body.setNewBalance(new BigDecimal("50"));
        when(redemptionService.redeem(eq("t1"), any())).thenReturn(body);

        mockMvc.perform(post("/api/v1/integration/t1/redemptions")
                .with(apiKeyAuth("t1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(redemptionRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void redeem_insufficientBalance_returns400() throws Exception {
        when(redemptionService.redeem(eq("t1"), any()))
            .thenThrow(new RewardInsufficientBalanceException(new BigDecimal("10"), new BigDecimal("50")));

        mockMvc.perform(post("/api/v1/integration/t1/redemptions")
                .with(apiKeyAuth("t1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(redemptionRequest())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void redeem_limitExceeded_returns400() throws Exception {
        when(redemptionService.redeem(eq("t1"), any()))
            .thenThrow(new RewardRedemptionLimitExceededException(
                "limits", Map.of("pointsToRedeem", "Minimum redemption is 100 points")));

        mockMvc.perform(post("/api/v1/integration/t1/redemptions")
                .with(apiKeyAuth("t1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(redemptionRequest())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("REDEMPTION_LIMIT_EXCEEDED"));
    }

    @Test
    void redeem_validationFailed_returns400() throws Exception {
        when(redemptionService.redeem(eq("t1"), any()))
            .thenThrow(new RewardRedemptionValidationException(
                "failed", Map.of("pointsToRedeem", "too low")));

        mockMvc.perform(post("/api/v1/integration/t1/redemptions")
                .with(apiKeyAuth("t1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(redemptionRequest())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getBalance_tenantMismatch_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/integration/t1/customers/c1/balance")
                .with(apiKeyAuth("other-tenant")))
            .andExpect(status().isForbidden());
    }

    private static IntegrationRedemptionRequest redemptionRequest() {
        IntegrationRedemptionRequest req = new IntegrationRedemptionRequest();
        req.setRedemptionId("red_test_1");
        req.setCustomerId("c1");
        req.setPointsToRedeem(new BigDecimal("50"));
        return req;
    }
}
