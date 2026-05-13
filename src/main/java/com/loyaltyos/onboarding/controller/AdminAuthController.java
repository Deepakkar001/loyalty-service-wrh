package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.AdminLoginRequest;
import com.loyaltyos.onboarding.dto.response.AdminLoginResponse;
import com.loyaltyos.onboarding.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "Admin Auth", description = "Platform admin authentication")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = Objects.requireNonNull(adminAuthService, "adminAuthService");
    }

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate platform admin and return JWT")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
}
