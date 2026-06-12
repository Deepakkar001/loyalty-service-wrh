package com.loyaltyos.access.controller;

import com.loyaltyos.access.dto.AdminUpdateModulesRequest;
import com.loyaltyos.access.dto.ModuleCatalogItemDto;
import com.loyaltyos.access.service.TenantEntitlementService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard/tenants")
@Tag(name = "Admin Tenant Modules")
@SecurityRequirement(name = "bearerAuth")
public class AdminTenantModulesController {

    private final TenantEntitlementService entitlementService;

    public AdminTenantModulesController(TenantEntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping("/{tenantId}/modules")
    @Operation(summary = "List module entitlements for a tenant")
    public List<ModuleCatalogItemDto> getModules(@PathVariable String tenantId) {
        return entitlementService.getTenantModules(tenantId);
    }

    @PutMapping("/{tenantId}/modules")
    @Operation(summary = "Update module entitlements for a tenant")
    public List<ModuleCatalogItemDto> updateModules(
        @PathVariable String tenantId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody AdminUpdateModulesRequest request
    ) {
        String adminUid = TenantJwt.adminUid(jwt);
        entitlementService.updateAdminModules(tenantId, request.getEnabledModuleKeys(), adminUid != null ? adminUid : "platform-admin");
        return entitlementService.getTenantModules(tenantId);
    }
}
