package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.domain.enums.BusinessCategoryStatus;
import com.loyaltyos.onboarding.dto.request.ApproveBusinessCategoryRequest;
import com.loyaltyos.onboarding.dto.request.DeactivateBusinessCategoryRequest;
import com.loyaltyos.onboarding.dto.request.RejectBusinessCategoryRequest;
import com.loyaltyos.onboarding.dto.response.AdminBusinessCategoryItem;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.IndustryModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * Admin moderation API for tenant-suggested industries (business categories).
 *
 * <p>Pending suggestions never appear in the public {@code /api/v1/onboarding/metadata}
 * dropdown until they're approved here.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/business-categories")
@Tag(name = "Admin Business Categories", description = "Industry suggestion moderation")
public class AdminBusinessCategoryController {

    private final IndustryModerationService moderationService;

    public AdminBusinessCategoryController(IndustryModerationService moderationService) {
        this.moderationService = Objects.requireNonNull(moderationService, "moderationService");
    }

    @GetMapping
    @Operation(summary = "List industries", description = "Optional ?status=PENDING_REVIEW|APPROVED|REJECTED filter")
    public ResponseEntity<List<AdminBusinessCategoryItem>> list(
            @RequestParam(name = "status", required = false) String statusParam) {

        BusinessCategoryStatus status = null;
        if (statusParam != null && !statusParam.isBlank()) {
            try {
                status = BusinessCategoryStatus.valueOf(statusParam.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Treat unknown filter as "no filter".
            }
        }
        return ResponseEntity.ok(moderationService.list(status));
    }

    @PostMapping("/{code}/approve")
    @Operation(summary = "Approve a pending industry suggestion")
    public ResponseEntity<AdminBusinessCategoryItem> approve(
            @PathVariable("code") String code,
            @Valid @RequestBody(required = false) ApproveBusinessCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String adminId = TenantJwt.adminUid(jwt);
        String adminRole = TenantJwt.role(jwt);
        String label = request != null ? request.getLabel() : null;
        Integer sortOrder = request != null ? request.getSortOrder() : null;
        return ResponseEntity.ok(moderationService.approve(code, label, sortOrder, adminId, adminRole));
    }

    @PostMapping("/{code}/reject")
    @Operation(summary = "Reject or revoke an industry",
            description = "Allowed from any non-rejected state. Forbidden for system-seeded categories — "
                    + "use deactivate instead.")
    public ResponseEntity<AdminBusinessCategoryItem> reject(
            @PathVariable("code") String code,
            @Valid @RequestBody RejectBusinessCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String adminId = TenantJwt.adminUid(jwt);
        String adminRole = TenantJwt.role(jwt);
        return ResponseEntity.ok(moderationService.reject(code, request.getReason(), adminId, adminRole));
    }

    @PostMapping("/{code}/deactivate")
    @Operation(summary = "Hide an approved industry from the public dropdown",
            description = "Keeps status=APPROVED but flips active=false. Tenants who already chose this "
                    + "category retain their selection; new tenants no longer see it.")
    public ResponseEntity<AdminBusinessCategoryItem> deactivate(
            @PathVariable("code") String code,
            @Valid @RequestBody(required = false) DeactivateBusinessCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String adminId = TenantJwt.adminUid(jwt);
        String adminRole = TenantJwt.role(jwt);
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(moderationService.deactivate(code, reason, adminId, adminRole));
    }

    @PostMapping("/{code}/reactivate")
    @Operation(summary = "Re-show a previously deactivated approved industry")
    public ResponseEntity<AdminBusinessCategoryItem> reactivate(
            @PathVariable("code") String code,
            @AuthenticationPrincipal Jwt jwt) {

        String adminId = TenantJwt.adminUid(jwt);
        String adminRole = TenantJwt.role(jwt);
        return ResponseEntity.ok(moderationService.reactivate(code, adminId, adminRole));
    }
}
