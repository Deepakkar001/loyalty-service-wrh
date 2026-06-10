package com.loyaltyos.coupon.controller;

import com.loyaltyos.coupon.dto.CouponRedeemRequest;
import com.loyaltyos.coupon.dto.CouponRedeemResponse;
import com.loyaltyos.coupon.dto.CouponValidateRequest;
import com.loyaltyos.coupon.dto.CouponValidateResponse;
import com.loyaltyos.coupon.service.CouponValidationService;
import com.loyaltyos.coupon.support.CouponCodeNormalizer;
import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationEventService;
import com.loyaltyos.integration.service.IntegrationMetricsService;
import com.loyaltyos.integration.support.IntegrationAuthSupport;
import com.loyaltyos.onboarding.service.ProgrammeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/integration/{tenantId}/coupons")
@Tag(name = "Integration Coupons", description = "Validate and redeem marketing coupons at checkout")
@ConditionalOnProperty(name = "loyaltyos.coupon.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationCouponController {

    private final ProgrammeService programmeService;
    private final CouponValidationService validationService;
    private final IntegrationAuditService auditService;
    private final IntegrationMetricsService metricsService;

    public IntegrationCouponController(
        ProgrammeService programmeService,
        CouponValidationService validationService,
        IntegrationAuditService auditService,
        IntegrationMetricsService metricsService
    ) {
        this.programmeService = Objects.requireNonNull(programmeService);
        this.validationService = Objects.requireNonNull(validationService);
        this.auditService = Objects.requireNonNull(auditService);
        this.metricsService = Objects.requireNonNull(metricsService);
    }

    @PostMapping("/{code}/validate")
    public ResponseEntity<CouponValidateResponse> validate(
        @PathVariable String tenantId,
        @PathVariable String code,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody CouponValidateRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        programmeService.assertProgrammeActiveForIntegration(tenantId, request.getProgrammeUid());

        CouponValidateResponse response = validationService.validate(tenantId, code, request);
        int status = response.isValid() ? 200 : 422;
        logPost(tenantId, auth, servletRequest, request.getCustomerId(), status, "coupons/validate");
        return response.isValid()
            ? ResponseEntity.ok(response)
            : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<CouponValidateResponse> details(
        @PathVariable String tenantId,
        @PathVariable String code,
        @RequestParam(defaultValue = "default") String programmeUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        programmeService.assertProgrammeActiveForIntegration(tenantId, programmeUid);

        CouponValidateResponse response = validationService.getDetails(tenantId, code, programmeUid);
        if (response.getReason() == com.loyaltyos.coupon.enums.CouponValidationReason.NOT_FOUND) {
            logGet(tenantId, auth, servletRequest, 404, "coupons/details");
            return ResponseEntity.notFound().build();
        }
        logGet(tenantId, auth, servletRequest, 200, "coupons/details");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<CouponRedeemResponse> redeem(
        @PathVariable String tenantId,
        @PathVariable String code,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody CouponRedeemRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        programmeService.assertProgrammeActiveForIntegration(tenantId, request.getProgrammeUid());

        CouponRedeemResponse response = validationService.redeem(tenantId, code, request);
        int status = switch (response.getStatus()) {
            case "SUCCESS" -> 201;
            case "VALIDATION_FAILED" -> 422;
            default -> 500;
        };
        logPost(tenantId, auth, servletRequest, request.getCustomerId(), status, "coupons/redeem");
        return switch (response.getStatus()) {
            case "SUCCESS" -> ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Idempotent-Replay", String.valueOf(response.isIdempotentReplay()))
                .body(response);
            case "VALIDATION_FAILED" -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            default -> ResponseEntity.internalServerError().body(response);
        };
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
