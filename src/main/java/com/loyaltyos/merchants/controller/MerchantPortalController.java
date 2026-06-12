package com.loyaltyos.merchants.controller;

import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.service.MerchantCampaignService;
import com.loyaltyos.merchants.service.MerchantOnboardingService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
@Tag(name = "Merchant Portal", description = "Merchant self-service APIs")
@ConditionalOnProperty(name = "loyaltyos.merchant.enabled", havingValue = "true", matchIfMissing = true)
public class MerchantPortalController {

    private final MerchantOnboardingService onboardingService;
    private final MerchantCampaignService merchantCampaignService;

    public MerchantPortalController(
        MerchantOnboardingService onboardingService,
        MerchantCampaignService merchantCampaignService
    ) {
        this.onboardingService = Objects.requireNonNull(onboardingService, "onboardingService");
        this.merchantCampaignService = Objects.requireNonNull(merchantCampaignService, "merchantCampaignService");
    }

    @GetMapping("/profile")
    public ResponseEntity<MerchantResponse> profile(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(onboardingService.get(tenantId, merchantUid));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponse>> listCampaigns(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(merchantCampaignService.listMerchantCampaigns(tenantId, merchantUid));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<CampaignResponse> createCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CampaignUpsertRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        String email = TenantJwt.email(jwt);
        CampaignResponse created = merchantCampaignService.createMerchantCampaign(
            tenantId, merchantUid, request, email != null ? email : merchantUid);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
