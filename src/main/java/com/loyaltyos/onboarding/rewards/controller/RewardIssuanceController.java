package com.loyaltyos.onboarding.rewards.controller;

import com.loyaltyos.onboarding.rewards.dto.RewardBalanceDetailResponse;
import com.loyaltyos.onboarding.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueRequest;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueResponse;
import com.loyaltyos.onboarding.rewards.dto.RewardReverseRequest;
import com.loyaltyos.onboarding.rewards.dto.RewardReverseResponse;
import com.loyaltyos.onboarding.rewards.service.RewardBalanceQueryService;
import com.loyaltyos.onboarding.rewards.service.RewardIssuanceService;
import com.loyaltyos.onboarding.rewards.service.RewardReversalService;
import com.loyaltyos.onboarding.security.TenantJwt;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
@Validated
public class RewardIssuanceController {

    private final RewardIssuanceService rewardIssuanceService;
    private final RewardBalanceQueryService rewardBalanceQueryService;
    private final RewardReversalService rewardReversalService;

    public RewardIssuanceController(
        RewardIssuanceService rewardIssuanceService,
        RewardBalanceQueryService rewardBalanceQueryService,
        RewardReversalService rewardReversalService
    ) {
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.rewardBalanceQueryService = Objects.requireNonNull(rewardBalanceQueryService, "rewardBalanceQueryService");
        this.rewardReversalService = Objects.requireNonNull(rewardReversalService, "rewardReversalService");
    }

    @PostMapping("/issue")
    public ResponseEntity<RewardIssueResponse> issue(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RewardIssueRequest body
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        RewardIssueResponse res = rewardIssuanceService.issue(tenantId, body);
        if (res.isIdempotentReplay()) {
            return ResponseEntity.ok(res);
        }
        if (res.getLedgerRowsCreated() == 0 && (body.getRewardCommands() == null || body.getRewardCommands().isEmpty())) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/balance")
    public ResponseEntity<RewardBalanceResponse> balance(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("customerId") String customerId,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(rewardIssuanceService.getBalance(tenantId, programmeUid, customerId));
    }

    @GetMapping("/balance-detail")
    public ResponseEntity<RewardBalanceDetailResponse> balanceDetail(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("customerId") String customerId,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(rewardBalanceQueryService.getBalanceDetail(tenantId, programmeUid, customerId));
    }

    @PostMapping("/reverse")
    public ResponseEntity<RewardReverseResponse> reverse(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody RewardReverseRequest body
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        RewardReverseResponse res = rewardReversalService.reverse(tenantId, body);
        if (res.isIdempotentReplay()) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
