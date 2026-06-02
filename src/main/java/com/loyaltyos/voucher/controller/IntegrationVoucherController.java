package com.loyaltyos.voucher.controller;

import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationEventService;
import com.loyaltyos.integration.service.IntegrationMetricsService;
import com.loyaltyos.integration.support.IntegrationAuthSupport;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.voucher.dto.VoucherIssueRequest;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import com.loyaltyos.voucher.dto.VoucherStockDto;
import com.loyaltyos.voucher.dto.VoucherValidateRequest;
import com.loyaltyos.voucher.exception.VoucherOutOfStockException;
import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.service.VoucherDenominationMappingService;
import com.loyaltyos.voucher.service.VoucherInventoryService;
import com.loyaltyos.voucher.service.VoucherIssueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration/{tenantId}/vouchers")
@Tag(name = "Integration Vouchers", description = "Issue inventory-backed voucher codes")
@ConditionalOnProperty(name = "loyaltyos.voucher.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationVoucherController {

    private final ProgrammeService programmeService;
    private final VoucherInventoryService inventoryService;
    private final VoucherIssueService issueService;
    private final VoucherDenominationMappingService denominationMappingService;
    private final IntegrationAuditService auditService;
    private final IntegrationMetricsService metricsService;

    public IntegrationVoucherController(
        ProgrammeService programmeService,
        VoucherInventoryService inventoryService,
        VoucherIssueService issueService,
        VoucherDenominationMappingService denominationMappingService,
        IntegrationAuditService auditService,
        IntegrationMetricsService metricsService
    ) {
        this.programmeService = Objects.requireNonNull(programmeService);
        this.inventoryService = Objects.requireNonNull(inventoryService);
        this.issueService = Objects.requireNonNull(issueService);
        this.denominationMappingService = Objects.requireNonNull(denominationMappingService);
        this.auditService = Objects.requireNonNull(auditService);
        this.metricsService = Objects.requireNonNull(metricsService);
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody VoucherValidateRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        programmeService.assertProgrammeActiveForIntegration(tenantId, request.getProgrammeUid());
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            if (denominationMappingService.hasActiveMappings(tenantId, request.getCatalogRewardUid())) {
                VoucherDenominationMapping mapping = denominationMappingService.resolveMapping(
                    tenantId,
                    request.getCatalogRewardUid(),
                    request.getPointsToRedeem(),
                    request.getFaceValue()
                );
                long available = inventoryService.countAvailableByFaceValue(
                    tenantId,
                    request.getProgrammeUid(),
                    request.getCatalogRewardUid(),
                    mapping.getFaceValue()
                );
                if (available == 0) {
                    body.put("status", "OUT_OF_STOCK");
                    body.put("available", 0);
                    body.put("message", "No stock for selected denomination");
                    body.put("selectedFaceValue", mapping.getFaceValue());
                    body.put("pointsRequired", mapping.getPointsRequired());
                    logPost(tenantId, auth, servletRequest, request.getCustomerId(), 409, "vouchers/validate");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
                }
                body.put("status", "SUCCESS");
                body.put("available", available);
                body.put("selectedFaceValue", mapping.getFaceValue());
                body.put("pointsRequired", mapping.getPointsRequired());
                body.put("message", "Voucher denomination available");
            } else {
                long available = inventoryService.countAvailable(
                    tenantId, request.getProgrammeUid(), request.getCatalogRewardUid()
                );
                if (available == 0) {
                    body.put("status", "OUT_OF_STOCK");
                    body.put("available", 0);
                    body.put("message", "No available vouchers");
                    logPost(tenantId, auth, servletRequest, request.getCustomerId(), 409, "vouchers/validate");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
                }
                body.put("status", "SUCCESS");
                body.put("available", available);
                body.put("message", "Voucher available for redemption");
            }
        } catch (VoucherCatalogException e) {
            body.put("status", "VALIDATION_FAILED");
            body.put("message", e.getMessage());
            logPost(tenantId, auth, servletRequest, request.getCustomerId(), 400, "vouchers/validate");
            return ResponseEntity.badRequest().body(body);
        }
        logPost(tenantId, auth, servletRequest, request.getCustomerId(), 200, "vouchers/validate");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issue(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody VoucherIssueRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        programmeService.assertProgrammeActiveForIntegration(tenantId, request.getProgrammeUid());

        try {
            VoucherIssueResponse response = issueService.issueVoucher(
                tenantId,
                request.getProgrammeUid(),
                request.getCatalogRewardUid(),
                request.getCustomerId(),
                request.getRedemptionId(),
                request.getPointsToRedeem(),
                request.getFaceValue()
            );

            return switch (response.getStatus()) {
                case "SUCCESS" -> {
                    logPost(tenantId, auth, servletRequest, request.getCustomerId(), 201, "vouchers/issue");
                    yield ResponseEntity.status(HttpStatus.CREATED)
                        .header("X-Idempotent-Replay", String.valueOf(response.isIdempotentReplay()))
                        .body(response);
                }
                case "OUT_OF_STOCK" -> {
                    logPost(tenantId, auth, servletRequest, request.getCustomerId(), 409, "vouchers/issue");
                    yield ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
                case "INSUFFICIENT_BALANCE" -> {
                    logPost(tenantId, auth, servletRequest, request.getCustomerId(), 400, "vouchers/issue");
                    yield ResponseEntity.badRequest().body(response);
                }
                default -> {
                    logPost(tenantId, auth, servletRequest, request.getCustomerId(), 500, "vouchers/issue");
                    yield ResponseEntity.internalServerError().body(response);
                }
            };
        } catch (VoucherOutOfStockException e) {
            VoucherIssueResponse response = new VoucherIssueResponse();
            response.setStatus("OUT_OF_STOCK");
            response.setErrorMessage(e.getMessage());
            response.setRetryable(false);
            response.setTimestamp(Instant.now());
            logPost(tenantId, auth, servletRequest, request.getCustomerId(), 409, "vouchers/issue");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (VoucherCatalogException e) {
            VoucherIssueResponse response = new VoucherIssueResponse();
            response.setStatus("VALIDATION_FAILED");
            response.setErrorMessage(e.getMessage());
            response.setRetryable(false);
            response.setTimestamp(Instant.now());
            logPost(tenantId, auth, servletRequest, request.getCustomerId(), 400, "vouchers/issue");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stock/{catalogRewardUid}")
    public ResponseEntity<VoucherStockDto> stock(
        @PathVariable String tenantId,
        @PathVariable String catalogRewardUid,
        @RequestParam(defaultValue = "default") String programmeUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        programmeService.assertProgrammeActiveForIntegration(tenantId, programmeUid);
        VoucherStockDto body = inventoryService.stock(tenantId, programmeUid, catalogRewardUid);
        logGet(tenantId, auth, servletRequest, 200, "vouchers/stock");
        return ResponseEntity.ok(body);
    }

    private void logPost(
        String tenantId,
        ApiKeyPrincipal auth,
        HttpServletRequest servletRequest,
        String customerId,
        int status,
        String metricKey
    ) {
        String bodyRaw = IntegrationAuthSupport.attributeBody(servletRequest);
        auditService.logApiRequest(
            tenantId, auth.keyUid(), IntegrationAuthSupport.requestId(servletRequest), "POST",
            servletRequest.getRequestURI(), null, customerId, status, 0,
            null, null, IntegrationEventService.hashPayload(bodyRaw),
            IntegrationAuthSupport.clientIp(servletRequest), servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, metricKey, status, 0);
    }

    private void logGet(
        String tenantId,
        ApiKeyPrincipal auth,
        HttpServletRequest servletRequest,
        int status,
        String metricKey
    ) {
        auditService.logApiRequest(
            tenantId, auth.keyUid(), IntegrationAuthSupport.requestId(servletRequest), "GET",
            servletRequest.getRequestURI(), null, null, status, 0,
            null, null, IntegrationEventService.hashPayload(""),
            IntegrationAuthSupport.clientIp(servletRequest), servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, metricKey, status, 0);
    }
}
