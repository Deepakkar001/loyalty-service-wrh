package com.loyaltyos.support.controller;

import com.loyaltyos.support.dto.SupportCaseResponse;
import com.loyaltyos.support.dto.UpdateSupportCaseStatusRequest;
import com.loyaltyos.support.enums.SupportCaseStatus;
import com.loyaltyos.support.service.SupportCaseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/support")
public class AdminSupportController {

    private final SupportCaseService supportCaseService;

    public AdminSupportController(SupportCaseService supportCaseService) {
        this.supportCaseService = Objects.requireNonNull(supportCaseService);
    }

    @GetMapping("/cases")
    public ResponseEntity<List<SupportCaseResponse>> listCases(
        @RequestParam(required = false) SupportCaseStatus status
    ) {
        return ResponseEntity.ok(supportCaseService.listCasesForAdmin(status));
    }

    @GetMapping("/cases/{caseUid}")
    public ResponseEntity<SupportCaseResponse> getCase(@PathVariable String caseUid) {
        return ResponseEntity.ok(supportCaseService.getCaseForAdmin(caseUid));
    }

    @PatchMapping("/cases/{caseUid}/status")
    public ResponseEntity<SupportCaseResponse> updateStatus(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String caseUid,
        @Valid @RequestBody UpdateSupportCaseStatusRequest request
    ) {
        return ResponseEntity.ok(supportCaseService.updateStatusForAdmin(caseUid, request, jwt));
    }
}
