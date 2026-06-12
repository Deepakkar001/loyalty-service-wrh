package com.loyaltyos.access.controller;

import com.loyaltyos.access.dto.ModuleCatalogResponse;
import com.loyaltyos.access.dto.SaveModulesRequest;
import com.loyaltyos.access.service.TenantEntitlementService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding/modules")
@Tag(name = "Onboarding Modules", description = "Module selection during onboarding")
@SecurityRequirement(name = "bearerAuth")
public class OnboardingModulesController {

    private final TenantEntitlementService entitlementService;

    public OnboardingModulesController(TenantEntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping("/catalog")
    @Operation(summary = "Get module catalog for onboarding picker")
    public ModuleCatalogResponse getCatalog(@AuthenticationPrincipal Jwt jwt) {
        return entitlementService.getCatalog(TenantJwt.requireTenantId(jwt));
    }

    @PostMapping
    @Operation(summary = "Save selected modules during onboarding")
    public ModuleCatalogResponse saveModules(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody SaveModulesRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        entitlementService.saveOnboardingModules(tenantId, request);
        return entitlementService.getCatalog(tenantId);
    }
}
