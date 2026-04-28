package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.response.AdminDashboardStats;
import com.loyaltyos.onboarding.dto.response.AdminTenantDetail;
import com.loyaltyos.onboarding.dto.response.AdminTenantListItem;
import com.loyaltyos.onboarding.dto.response.AuditLogItem;
import com.loyaltyos.onboarding.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin analytics and tenant management")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics", description = "Returns aggregate counts for the admin overview")
    public ResponseEntity<AdminDashboardStats> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/tenants")
    @Operation(summary = "List all tenants", description = "Returns all tenants ordered by creation date")
    public ResponseEntity<List<AdminTenantListItem>> listTenants() {
        return ResponseEntity.ok(dashboardService.listAllTenants());
    }

    @GetMapping("/tenants/{tenantId}")
    @Operation(summary = "Tenant detail", description = "Returns full tenant information including contacts and agreements")
    public ResponseEntity<AdminTenantDetail> getTenantDetail(@PathVariable String tenantId) {
        return ResponseEntity.ok(dashboardService.getTenantDetail(tenantId));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Recent audit logs", description = "Returns latest audit log entries")
    public ResponseEntity<List<AuditLogItem>> getAuditLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentAuditLogs(Math.min(limit, 200)));
    }

    @GetMapping("/tenants/{tenantId}/audit-logs")
    @Operation(summary = "Tenant audit logs", description = "Returns audit logs for a specific tenant")
    public ResponseEntity<List<AuditLogItem>> getTenantAuditLogs(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(dashboardService.getTenantAuditLogs(tenantId, Math.min(limit, 200)));
    }

    @GetMapping("/agreements/all")
    @Operation(summary = "List all agreements", description = "Returns all agreements across all tenants")
    public ResponseEntity<List<AdminDashboardService.PendingAgreementListItemInternal>> listAllAgreements() {
        return ResponseEntity.ok(dashboardService.listAllAgreements());
    }
}
