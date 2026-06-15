package com.loyaltyos.access.controller;

import com.loyaltyos.access.dto.MeAccessResponse;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.service.AccessProvisioningService;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/access")
@Tag(name = "Access", description = "Tenant access and navigation")
@SecurityRequirement(name = "bearerAuth")
public class MeAccessController {

    private final AccessResolutionService accessResolutionService;
    private final AccessProvisioningService accessProvisioningService;
    private final TenantUserRepository userRepository;

    public MeAccessController(
        AccessResolutionService accessResolutionService,
        AccessProvisioningService accessProvisioningService,
        TenantUserRepository userRepository
    ) {
        this.accessResolutionService = accessResolutionService;
        this.accessProvisioningService = accessProvisioningService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Resolve effective permissions and navigation for current user")
    public MeAccessResponse getMyAccess(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String tenantUserId = TenantJwt.tenantUserId(jwt);
        if (tenantUserId == null || tenantUserId.isBlank()) {
            String email = TenantJwt.email(jwt);
            tenantUserId = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
                .map(u -> u.getUserId())
                .orElseThrow(() -> new IllegalStateException("Tenant user not found"));
        }
        accessProvisioningService.prepareUserAccessContext(
            tenantId,
            tenantUserId,
            TenantJwt.email(jwt)
        );
        return accessResolutionService.resolveForUser(tenantId, tenantUserId);
    }
}
