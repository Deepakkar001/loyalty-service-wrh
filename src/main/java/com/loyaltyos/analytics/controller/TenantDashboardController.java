package com.loyaltyos.analytics.controller;

import com.loyaltyos.analytics.dto.DashboardOverviewResponse;
import com.loyaltyos.analytics.service.DashboardService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/dashboard")
@Tag(name = "Tenant Dashboard", description = "Executive dashboard overview for tenant portal")
public class TenantDashboardController {

    private final DashboardService dashboardService;

    public TenantDashboardController(DashboardService dashboardService) {
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
    }

    @GetMapping("/overview")
    @Operation(summary = "Dashboard overview", description = "Aggregated KPIs and charts for the tenant home dashboard")
    public ResponseEntity<DashboardOverviewResponse> overview(
        Authentication authentication,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        if ((fromDate == null) != (toDate == null)) {
            throw new IllegalArgumentException("fromDate and toDate must both be provided");
        }
        return ResponseEntity.ok(
            dashboardService.getOverview(
                TenantJwt.requireTenantId(authentication),
                programmeUid,
                fromDate,
                toDate
            )
        );
    }
}
