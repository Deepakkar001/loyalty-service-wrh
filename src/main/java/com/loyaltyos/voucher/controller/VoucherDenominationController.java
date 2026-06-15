package com.loyaltyos.voucher.controller;

import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.voucher.dto.DenominationMappingsResponse;
import com.loyaltyos.voucher.dto.ReplaceDenominationMappingsRequest;
import com.loyaltyos.voucher.dto.VoucherStockBreakdownDto;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.service.VoucherDenominationMappingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/vouchers/denominations")
@Tag(name = "Voucher Denominations", description = "Points-to-voucher denomination mapping for mixed bulk uploads")
@ConditionalOnProperty(name = "loyaltyos.voucher.enabled", havingValue = "true", matchIfMissing = true)
public class VoucherDenominationController {

    private static final Logger log = LoggerFactory.getLogger(VoucherDenominationController.class);

    private final VoucherDenominationMappingService denominationMappingService;

    public VoucherDenominationController(VoucherDenominationMappingService denominationMappingService) {
        this.denominationMappingService = Objects.requireNonNull(denominationMappingService);
    }

    @PutMapping
    @PreAuthorize("hasPermission(null, 'voucher_programs.edit')")
    public ResponseEntity<DenominationMappingsResponse> replaceMappings(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ReplaceDenominationMappingsRequest request,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        log.info(
            "Replace denomination mappings tenant={} catalog={} tiers={}",
            tenantId, request.getCatalogRewardUid(), request.getMappings().size()
        );
        try {
            denominationMappingService.replaceMappings(
                tenantId,
                programmeUid,
                request.getCatalogRewardUid(),
                request.getMappings()
            );
            return ResponseEntity.ok(
                denominationMappingService.getMappingsResponse(tenantId, programmeUid, request.getCatalogRewardUid())
            );
        } catch (IllegalArgumentException | VoucherCatalogException e) {
            throw e;
        }
    }

    @GetMapping("/{catalogRewardUid}")
    public ResponseEntity<DenominationMappingsResponse> getMappings(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String catalogRewardUid,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(
            denominationMappingService.getMappingsResponse(tenantId, programmeUid, catalogRewardUid)
        );
    }

    @GetMapping("/{catalogRewardUid}/stock")
    public ResponseEntity<VoucherStockBreakdownDto> stockBreakdown(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String catalogRewardUid,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(
            denominationMappingService.stockBreakdown(tenantId, programmeUid, catalogRewardUid)
        );
    }
}
