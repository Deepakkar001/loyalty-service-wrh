package com.loyaltyos.merchants.controller;

import com.loyaltyos.merchants.dto.MerchantAuthRequest;
import com.loyaltyos.merchants.dto.MerchantAuthResponse;
import com.loyaltyos.merchants.dto.MerchantChangePasswordRequest;
import com.loyaltyos.merchants.service.MerchantAuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/auth")
@Tag(name = "Merchant Auth", description = "Merchant portal authentication")
@ConditionalOnProperty(name = "loyaltyos.merchant.enabled", havingValue = "true", matchIfMissing = true)
public class MerchantAuthController {

    private final MerchantAuthenticationService authenticationService;

    public MerchantAuthController(MerchantAuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
    }

    @PostMapping("/login")
    public ResponseEntity<MerchantAuthResponse> login(@Valid @RequestBody MerchantAuthRequest request) {
        MerchantAuthResponse response = authenticationService.authenticate(
            request.getUsername(),
            request.getPassword(),
            request.getTenantId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<MerchantAuthResponse> changePassword(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MerchantChangePasswordRequest request
    ) {
        return ResponseEntity.ok(authenticationService.changePassword(jwt, request));
    }
}
