package com.loyaltyos.onboarding.rules.controller;

import com.loyaltyos.onboarding.rules.dto.EarnRuleResponse;
import com.loyaltyos.onboarding.rules.dto.EarnRuleDetailResponse;
import com.loyaltyos.onboarding.rules.dto.RuleChangeLogResponse;
import com.loyaltyos.onboarding.rules.dto.RuleStatusPatchRequest;
import com.loyaltyos.onboarding.rules.dto.RuleUpsertRequest;
import com.loyaltyos.onboarding.rules.service.EarnRuleAdminService;
import com.loyaltyos.onboarding.security.TenantJwt;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/engine/rule/admin")
@Validated
public class RuleAdminController {

    private final EarnRuleAdminService earnRuleAdminService;

    public RuleAdminController(EarnRuleAdminService earnRuleAdminService) {
        this.earnRuleAdminService = Objects.requireNonNull(earnRuleAdminService, "earnRuleAdminService");
    }

    @PostMapping("/rules")
    public ResponseEntity<EarnRuleResponse> createRule(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RuleUpsertRequest body
    ) {
        String tenantId = requireTenant(jwt);
        String programmeUid = body.getProgrammeUid() == null || body.getProgrammeUid().isBlank()
            ? "default"
            : body.getProgrammeUid();
        return ResponseEntity.ok(earnRuleAdminService.createRule(tenantId, programmeUid, body, tenantId));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<EarnRuleResponse>> listRules(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam(value = "ruleType", required = false) String ruleType
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(earnRuleAdminService.listRules(tenantId, programmeUid, ruleType));
    }

    @GetMapping("/rules/{ruleUid}")
    public ResponseEntity<EarnRuleDetailResponse> getRule(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("ruleUid") String ruleUid,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(earnRuleAdminService.getRule(tenantId, programmeUid, ruleUid));
    }

    @GetMapping("/rules/{ruleUid}/change-history")
    public ResponseEntity<List<RuleChangeLogResponse>> getChangeHistory(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("ruleUid") String ruleUid,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(earnRuleAdminService.getChangeHistory(tenantId, programmeUid, ruleUid));
    }

    @PutMapping("/rules/{ruleUid}")
    public ResponseEntity<EarnRuleResponse> updateRule(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("ruleUid") String ruleUid,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @Valid @RequestBody RuleUpsertRequest body
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(earnRuleAdminService.updateRule(tenantId, programmeUid, ruleUid, body, tenantId));
    }

    @DeleteMapping("/rules/{ruleUid}")
    public ResponseEntity<Void> deleteRule(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("ruleUid") String ruleUid,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = requireTenant(jwt);
        earnRuleAdminService.deleteRule(tenantId, programmeUid, ruleUid, tenantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{ruleUid}/status")
    public ResponseEntity<EarnRuleResponse> patchStatus(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("ruleUid") String ruleUid,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @Valid @RequestBody RuleStatusPatchRequest body
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(earnRuleAdminService.updateStatus(tenantId, programmeUid, ruleUid, body, tenantId));
    }

    private static String requireTenant(Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Missing tenant");
        }
        return tenantId;
    }
}
