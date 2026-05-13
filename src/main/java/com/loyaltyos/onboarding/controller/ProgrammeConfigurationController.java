package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.ProgrammeConfigRequest;
import com.loyaltyos.onboarding.dto.response.ProgrammeConfigResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Programme Configuration", description = "Stage 4: Configure programme, tiers, and webhook")
public class ProgrammeConfigurationController {

    private final TenantConfigService tenantConfigService;

    public ProgrammeConfigurationController(TenantConfigService tenantConfigService) {
        this.tenantConfigService = Objects.requireNonNull(tenantConfigService, "tenantConfigService");
    }

    @PostMapping("/api/v1/onboarding/{tenantId}/configuration")
    @Operation(summary = "Legacy: Save programme configuration (Stage 4)",
        description = "Legacy endpoint required by existing frontend Step4Programme.tsx. TenantId in path must match JWT.")
    public ResponseEntity<ProgrammeConfigResponse> saveLegacy(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("tenantId") String tenantId,
        @Valid @RequestBody ProgrammeConfigRequest request
    ) {
        String jwtTenantId = TenantJwt.tenantId(jwt);
        if (jwtTenantId == null || !jwtTenantId.equals(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(tenantConfigService.saveLegacyProgrammeConfiguration(tenantId, request));
    }

    @GetMapping("/api/v1/onboarding/{tenantId}/configuration")
    @Operation(summary = "Legacy: Get programme configuration (Stage 4)",
        description = "Legacy endpoint required by existing frontend Step4Programme.tsx. TenantId in path must match JWT.")
    public ResponseEntity<ProgrammeConfigResponse> getLegacy(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("tenantId") String tenantId
    ) {
        String jwtTenantId = TenantJwt.tenantId(jwt);
        if (jwtTenantId == null || !jwtTenantId.equals(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(tenantConfigService.getLegacyProgrammeConfiguration(tenantId));
    }

    @GetMapping("/api/v1/me/config")
    @Operation(summary = "Get my tenant configuration (Stage 4)",
        description = "Returns programme configuration and tiers for authenticated tenant.")
    public ResponseEntity<ProgrammeConfigResponse> getMyConfig(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(tenantConfigService.getLegacyProgrammeConfiguration(tenantId));
    }

    @PostMapping("/api/v1/me/config")
    @Operation(summary = "Save my tenant configuration (Stage 4)",
        description = "Saves programme configuration and tiers for authenticated tenant.")
    public ResponseEntity<ProgrammeConfigResponse> saveMyConfig(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ProgrammeConfigRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(tenantConfigService.saveLegacyProgrammeConfiguration(tenantId, request));
    }
}

