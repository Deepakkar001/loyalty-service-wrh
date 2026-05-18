package com.loyaltyos.campaigns.controller;

import com.loyaltyos.campaigns.dto.LoyaltyEventProcessRequest;
import com.loyaltyos.campaigns.dto.LoyaltyEventProcessResponse;
import com.loyaltyos.campaigns.service.CampaignOrchestrationService;
import com.loyaltyos.onboarding.security.TenantJwt;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty/events")
@Validated
public class LoyaltyEventProcessController {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyEventProcessController.class);

    private final CampaignOrchestrationService orchestrationService;

    public LoyaltyEventProcessController(CampaignOrchestrationService orchestrationService) {
        this.orchestrationService = Objects.requireNonNull(orchestrationService, "orchestrationService");
    }

    @PostMapping("/process")
    public ResponseEntity<LoyaltyEventProcessResponse> process(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody LoyaltyEventProcessRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.status(403).build();
        }
        log.info(
            "Loyalty event process tenant={} customer={} event={}",
            tenantId,
            request.getCustomerId(),
            request.getTransactionId()
        );
        LoyaltyEventProcessResponse response = orchestrationService.process(tenantId, request);
        return ResponseEntity.ok(response);
    }
}
