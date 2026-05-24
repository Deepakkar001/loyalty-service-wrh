package com.loyaltyos.integration.controller;

import com.loyaltyos.integration.dto.IntegrationBalanceDetailResponse;
import com.loyaltyos.integration.dto.IntegrationBalanceResponse;
import com.loyaltyos.integration.dto.IntegrationRedemptionRequest;
import com.loyaltyos.integration.dto.IntegrationRedemptionResponse;
import com.loyaltyos.integration.dto.IntegrationRedemptionValidationResponse;
import com.loyaltyos.integration.dto.IntegrationTransactionResponse;
import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationBalanceService;
import com.loyaltyos.integration.service.IntegrationEventService;
import com.loyaltyos.integration.service.IntegrationMetricsService;
import com.loyaltyos.integration.service.IntegrationRedemptionService;
import com.loyaltyos.integration.support.IntegrationAuthSupport;
import com.loyaltyos.rules.enums.LedgerEntryType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;

/**
 * Public integration APIs for balance inquiry and redemption.
 * Reuses {@link com.loyaltyos.rewards.service.RewardIssuanceService},
 * {@link com.loyaltyos.rewards.service.RewardBalanceQueryService},
 * {@link com.loyaltyos.rewards.service.PointsLedgerQueryService}, and
 * {@link com.loyaltyos.rewards.service.RewardRedemptionService} — see docs/INTEGRATION_API_GUIDE.md.
 */
@RestController
@RequestMapping("/api/v1/integration/{tenantId}")
@Tag(name = "Integration Rewards API", description = "Balance inquiry and redemption for tenant backends")
@SuppressWarnings("null")
public class IntegrationRewardsController {

    private final IntegrationBalanceService balanceService;
    private final IntegrationRedemptionService redemptionService;
    private final IntegrationAuditService auditService;
    private final IntegrationMetricsService metricsService;

    public IntegrationRewardsController(
        IntegrationBalanceService balanceService,
        IntegrationRedemptionService redemptionService,
        IntegrationAuditService auditService,
        IntegrationMetricsService metricsService
    ) {
        this.balanceService = Objects.requireNonNull(balanceService, "balanceService");
        this.redemptionService = Objects.requireNonNull(redemptionService, "redemptionService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService");
    }

    @GetMapping("/customers/{customerId}/balance")
    public ResponseEntity<IntegrationBalanceResponse> getBalance(
        @PathVariable String tenantId,
        @PathVariable String customerId,
        @RequestParam(defaultValue = "default") String programmeUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        long start = System.currentTimeMillis();
        IntegrationBalanceResponse body = balanceService.getBalance(tenantId, programmeUid, customerId);
        int processingMs = (int) (System.currentTimeMillis() - start);
        logGet(tenantId, auth, servletRequest, customerId, null, "customers/balance", 200, processingMs);
        return ResponseEntity.ok()
            .header("X-Request-ID", IntegrationAuthSupport.requestId(servletRequest))
            .header("X-Processing-Time", String.valueOf(processingMs))
            .body(body);
    }

    @GetMapping("/customers/{customerId}/balance-detail")
    public ResponseEntity<IntegrationBalanceDetailResponse> getBalanceDetail(
        @PathVariable String tenantId,
        @PathVariable String customerId,
        @RequestParam(defaultValue = "default") String programmeUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        long start = System.currentTimeMillis();
        IntegrationBalanceDetailResponse body = balanceService.getBalanceDetail(tenantId, programmeUid, customerId);
        int processingMs = (int) (System.currentTimeMillis() - start);
        logGet(tenantId, auth, servletRequest, customerId, null, "customers/balance-detail", 200, processingMs);
        return ResponseEntity.ok()
            .header("X-Request-ID", IntegrationAuthSupport.requestId(servletRequest))
            .header("X-Processing-Time", String.valueOf(processingMs))
            .body(body);
    }

    @GetMapping("/customers/{customerId}/transactions")
    public ResponseEntity<Page<IntegrationTransactionResponse>> listTransactions(
        @PathVariable String tenantId,
        @PathVariable String customerId,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) LedgerEntryType entryType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        long start = System.currentTimeMillis();
        Page<IntegrationTransactionResponse> body = balanceService.listTransactions(
            tenantId, programmeUid, customerId, entryType, from, to, PageRequest.of(page, size)
        );
        int processingMs = (int) (System.currentTimeMillis() - start);
        logGet(tenantId, auth, servletRequest, customerId, null, "customers/transactions", 200, processingMs);
        return ResponseEntity.ok()
            .header("X-Request-ID", IntegrationAuthSupport.requestId(servletRequest))
            .header("X-Processing-Time", String.valueOf(processingMs))
            .body(body);
    }

    @PostMapping("/redemptions/validate")
    public ResponseEntity<IntegrationRedemptionValidationResponse> validateRedemption(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody IntegrationRedemptionRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        long start = System.currentTimeMillis();
        IntegrationRedemptionValidationResponse body = redemptionService.validate(tenantId, request);
        int processingMs = (int) (System.currentTimeMillis() - start);
        String payloadHash = IntegrationEventService.hashPayload(IntegrationAuthSupport.attributeBody(servletRequest));
        auditService.logApiRequest(
            tenantId, auth.keyUid(), IntegrationAuthSupport.requestId(servletRequest), "POST",
            servletRequest.getRequestURI(), request.getRedemptionId(), request.getCustomerId(),
            200, processingMs, null, null, payloadHash,
            IntegrationAuthSupport.clientIp(servletRequest), servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, "redemptions/validate", 200, processingMs);
        return ResponseEntity.ok()
            .header("X-Processing-Time", String.valueOf(processingMs))
            .body(body);
    }

    @PostMapping("/redemptions")
    public ResponseEntity<IntegrationRedemptionResponse> redeem(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody IntegrationRedemptionRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        long start = System.currentTimeMillis();
        String bodyRaw = IntegrationAuthSupport.attributeBody(servletRequest);
        String payloadHash = IntegrationEventService.hashPayload(bodyRaw);
        try {
            IntegrationRedemptionResponse body = redemptionService.redeem(tenantId, request);
            int processingMs = (int) (System.currentTimeMillis() - start);
            auditService.logApiRequest(
                tenantId, auth.keyUid(), IntegrationAuthSupport.requestId(servletRequest), "POST",
                servletRequest.getRequestURI(), request.getRedemptionId(), request.getCustomerId(),
                200, processingMs, null, null, payloadHash,
                IntegrationAuthSupport.clientIp(servletRequest), servletRequest.getHeader("User-Agent")
            );
            metricsService.recordRequest(tenantId, "redemptions", 200, processingMs);
            return ResponseEntity.ok()
                .header("X-Request-ID", IntegrationAuthSupport.requestId(servletRequest))
                .header("X-Processing-Time", String.valueOf(processingMs))
                .body(body);
        } catch (RuntimeException ex) {
            int processingMs = (int) (System.currentTimeMillis() - start);
            metricsService.recordRequest(tenantId, "redemptions", 400, processingMs);
            throw ex;
        }
    }

    private void logGet(
        String tenantId,
        ApiKeyPrincipal auth,
        HttpServletRequest servletRequest,
        String customerId,
        String eventId,
        String metricKey,
        int httpStatus,
        int processingMs
    ) {
        auditService.logApiRequest(
            tenantId, auth.keyUid(), IntegrationAuthSupport.requestId(servletRequest), "GET",
            servletRequest.getRequestURI(), eventId, customerId, httpStatus, processingMs,
            null, null, IntegrationEventService.hashPayload(""),
            IntegrationAuthSupport.clientIp(servletRequest), servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, metricKey, httpStatus, processingMs);
    }
}
