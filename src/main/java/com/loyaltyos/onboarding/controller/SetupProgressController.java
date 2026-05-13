package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.SetupProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/setup")
@Tag(name = "Setup Progress", description = "Guided setup progression for tenant dashboard")
public class SetupProgressController {

    private final SetupProgressService setupProgressService;

    public SetupProgressController(SetupProgressService setupProgressService) {
        this.setupProgressService = Objects.requireNonNull(setupProgressService, "setupProgressService");
    }

    @PostMapping("/rules/complete")
    @Operation(summary = "Mark rules setup complete and unlock integration")
    public ResponseEntity<Void> completeRules(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        setupProgressService.completeRules(tenantId);
        return ResponseEntity.accepted().build();
    }
}

