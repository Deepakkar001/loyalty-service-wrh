package com.loyaltyos.coupon.controller;

import com.loyaltyos.coupon.dto.CouponUsageReportResponse;
import com.loyaltyos.coupon.service.CouponAnalyticsService;
import com.loyaltyos.onboarding.security.TenantJwt;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/coupons/analytics")
@ConditionalOnProperty(name = "loyaltyos.coupon.enabled", havingValue = "true", matchIfMissing = true)
public class CouponAnalyticsController {

    private final CouponAnalyticsService analyticsService;

    public CouponAnalyticsController(CouponAnalyticsService analyticsService) {
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    @GetMapping("/usage-report")
    public ResponseEntity<CouponUsageReportResponse> usageReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam String programmeUid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            analyticsService.buildUsageReport(TenantJwt.tenantId(jwt), programmeUid, from, to)
        );
    }
}
