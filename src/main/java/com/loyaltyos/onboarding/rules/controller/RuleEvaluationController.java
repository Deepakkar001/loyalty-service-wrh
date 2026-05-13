package com.loyaltyos.onboarding.rules.controller;

import com.loyaltyos.onboarding.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.onboarding.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.onboarding.rules.service.RuleEvaluationService;
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
@RequestMapping("/api/v1/engine/rule")
@Validated
public class RuleEvaluationController {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluationController.class);

    private final RuleEvaluationService ruleEvaluationService;

    public RuleEvaluationController(RuleEvaluationService ruleEvaluationService) {
        this.ruleEvaluationService = Objects.requireNonNull(ruleEvaluationService, "ruleEvaluationService");
    }

    @PostMapping("/evaluate")
    public ResponseEntity<RuleEvaluationResponse> evaluate(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RuleEvaluateRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.status(403).build();
        }
        log.info("Rule evaluation tenant={} customer={} event={}", tenantId, request.getCustomerId(), request.getEventId());
        RuleEvaluationResponse response = ruleEvaluationService.evaluate(tenantId, request);
        return ResponseEntity.ok(response);
    }
}
