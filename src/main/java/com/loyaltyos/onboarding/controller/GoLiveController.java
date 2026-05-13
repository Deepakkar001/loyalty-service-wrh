package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.response.GoLiveActivateResponse;
import com.loyaltyos.onboarding.dto.response.GoLiveChecklistResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.GoLiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Go-Live", description = "Stage 6: Checklist + activation")
public class GoLiveController {

    private final GoLiveService goLiveService;

    public GoLiveController(GoLiveService goLiveService) {
        this.goLiveService = Objects.requireNonNull(goLiveService, "goLiveService");
    }

    @GetMapping("/api/v1/me/go-live/checklist")
    @Operation(summary = "Get go-live preflight checklist")
    public ResponseEntity<GoLiveChecklistResponse> checklist(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(goLiveService.getChecklist(tenantId));
    }

    @PostMapping("/api/v1/me/go-live/activate")
    @Operation(summary = "Activate tenant (go-live)")
    public ResponseEntity<GoLiveActivateResponse> activate(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(goLiveService.activate(tenantId));
    }
}

