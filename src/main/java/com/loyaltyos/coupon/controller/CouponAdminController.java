package com.loyaltyos.coupon.controller;

import com.loyaltyos.coupon.dto.CouponCreateRequest;
import com.loyaltyos.coupon.dto.CouponRedemptionListItem;
import com.loyaltyos.coupon.dto.CouponResponse;
import com.loyaltyos.coupon.dto.CouponUpdateRequest;
import com.loyaltyos.coupon.exception.CouponAdminException;
import com.loyaltyos.coupon.exception.CouponNotFoundException;
import com.loyaltyos.coupon.service.CouponAdminService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me/coupons")
@Tag(name = "Coupon Admin", description = "Portal coupon management")
@ConditionalOnProperty(name = "loyaltyos.coupon.enabled", havingValue = "true", matchIfMissing = true)
public class CouponAdminController {

    private final CouponAdminService adminService;

    public CouponAdminController(CouponAdminService adminService) {
        this.adminService = Objects.requireNonNull(adminService);
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CouponCreateRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String createdBy = jwt.getSubject();
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.create(tenantId, createdBy, request));
        } catch (CouponAdminException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String programmeUid
    ) {
        return ResponseEntity.ok(adminService.list(TenantJwt.tenantId(jwt), programmeUid));
    }

    @GetMapping("/{couponUid}")
    public ResponseEntity<CouponResponse> get(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String couponUid
    ) {
        try {
            return ResponseEntity.ok(adminService.get(TenantJwt.tenantId(jwt), couponUid));
        } catch (CouponNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/{couponUid}")
    public ResponseEntity<CouponResponse> update(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String couponUid,
        @Valid @RequestBody CouponUpdateRequest request
    ) {
        try {
            return ResponseEntity.ok(adminService.update(TenantJwt.tenantId(jwt), couponUid, request));
        } catch (CouponNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (CouponAdminException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{couponUid}/activate")
    public ResponseEntity<CouponResponse> activate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String couponUid
    ) {
        try {
            return ResponseEntity.ok(adminService.activate(TenantJwt.tenantId(jwt), couponUid));
        } catch (CouponNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (CouponAdminException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{couponUid}/revoke")
    public ResponseEntity<CouponResponse> revoke(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String couponUid
    ) {
        try {
            return ResponseEntity.ok(adminService.revoke(TenantJwt.tenantId(jwt), couponUid));
        } catch (CouponNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{couponUid}/redemptions")
    public ResponseEntity<List<CouponRedemptionListItem>> redemptions(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String couponUid
    ) {
        try {
            return ResponseEntity.ok(adminService.listRedemptions(TenantJwt.tenantId(jwt), couponUid));
        } catch (CouponNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> types() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("couponTypes", List.of(
            "FIXED_DISCOUNT", "PCT_DISCOUNT", "FREE_ITEM", "CASHBACK", "POINTS_BONUS"
        ));
        body.put("usageTypes", List.of("SINGLE_USE", "MULTI_USE"));
        body.put("statuses", List.of("DRAFT", "ACTIVE", "EXPIRED", "REVOKED", "EXHAUSTED"));
        return ResponseEntity.ok(body);
    }
}
