package com.loyaltyos.referrals.controller;

import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.support.IntegrationAuthSupport;
import com.loyaltyos.referrals.dto.ReferralCodeResponse;
import com.loyaltyos.referrals.dto.ReferralLinkRequest;
import com.loyaltyos.referrals.dto.ReferralLinkResponse;
import com.loyaltyos.referrals.entity.ReferralCode;
import com.loyaltyos.referrals.service.ReferralCodeService;
import com.loyaltyos.referrals.service.ReferralLinkingService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration/{tenantId}/referrals")
public class IntegrationReferralController {

    private final ReferralCodeService codeService;
    private final ReferralLinkingService linkingService;

    public IntegrationReferralController(ReferralCodeService codeService, ReferralLinkingService linkingService) {
        this.codeService = Objects.requireNonNull(codeService, "codeService");
        this.linkingService = Objects.requireNonNull(linkingService, "linkingService");
    }

    @GetMapping("/code")
    public ResponseEntity<ReferralCodeResponse> getOrCreateCode(
        @org.springframework.web.bind.annotation.PathVariable String tenantId,
        @RequestParam String customerId,
        @RequestParam(defaultValue = "default") String programmeUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        ReferralCode code = codeService.getOrGenerateCode(tenantId, customerId, programmeUid);
        ReferralCodeResponse response = new ReferralCodeResponse();
        response.setProgrammeUid(code.getProgrammeUid());
        response.setCustomerId(code.getCustomerId());
        response.setCode(code.getCode());
        response.setStatus(code.getStatus().name());
        response.setCreatedAt(code.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/link")
    public ResponseEntity<ReferralLinkResponse> linkReferee(
        @org.springframework.web.bind.annotation.PathVariable String tenantId,
        @Valid @RequestBody ReferralLinkRequest request,
        @AuthenticationPrincipal ApiKeyPrincipal auth
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        ReferralLinkResponse response = linkingService.linkReferee(
            tenantId,
            request.getProgrammeUid(),
            request.getReferralCode(),
            request.getRefereeCustomerId(),
            request.getReferrerSignals(),
            request.getRefereeSignals()
        );
        return ResponseEntity.ok(response);
    }
}
