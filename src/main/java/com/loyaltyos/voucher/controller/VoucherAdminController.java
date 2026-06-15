package com.loyaltyos.voucher.controller;

import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherBatchDetailResponse;
import com.loyaltyos.voucher.dto.VoucherBatchListDto;
import com.loyaltyos.voucher.dto.VoucherBatchUploadResponse;
import com.loyaltyos.voucher.dto.VoucherStockDto;
import com.loyaltyos.voucher.dto.VoucherUploadSpecResponse;
import com.loyaltyos.voucher.support.VoucherUploadSpec;
import com.loyaltyos.voucher.entity.VoucherBatch;
import com.loyaltyos.voucher.exception.VoucherBatchUploadException;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.repository.VoucherBatchRepository;
import com.loyaltyos.voucher.service.VoucherBatchUploadService;
import com.loyaltyos.voucher.service.VoucherInventoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me/vouchers")
@Tag(name = "Voucher Admin", description = "Portal voucher batch upload and stock")
@ConditionalOnProperty(name = "loyaltyos.voucher.enabled", havingValue = "true", matchIfMissing = true)
public class VoucherAdminController {

    private static final Logger log = LoggerFactory.getLogger(VoucherAdminController.class);

    private final VoucherBatchUploadService batchUploadService;
    private final VoucherInventoryService inventoryService;
    private final VoucherBatchRepository batchRepository;
    private final VoucherProperties voucherProperties;
    private final ObjectMapper objectMapper;

    public VoucherAdminController(
        VoucherBatchUploadService batchUploadService,
        VoucherInventoryService inventoryService,
        VoucherBatchRepository batchRepository,
        VoucherProperties voucherProperties,
        ObjectMapper objectMapper
    ) {
        this.batchUploadService = Objects.requireNonNull(batchUploadService);
        this.inventoryService = Objects.requireNonNull(inventoryService);
        this.batchRepository = Objects.requireNonNull(batchRepository);
        this.voucherProperties = Objects.requireNonNull(voucherProperties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Platform-defined CSV contract: column list + example file content for tenants to download and fill in.
     */
    @GetMapping("/upload-spec")
    public ResponseEntity<VoucherUploadSpecResponse> uploadSpec() {
        return ResponseEntity.ok(VoucherUploadSpec.build(voucherProperties));
    }

    @PostMapping("/batches/upload")
    @PreAuthorize("hasPermission(null, 'voucher_programs.create')")
    public ResponseEntity<VoucherBatchUploadResponse> uploadBatch(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam String programmeUid,
        @RequestParam String catalogRewardUid,
        @RequestParam(required = false) String partnerUid,
        @RequestParam("file") MultipartFile file
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        log.info(
            "Voucher batch upload tenant={} programme={} catalog={} file={}",
            tenantId, programmeUid, catalogRewardUid, file.getOriginalFilename()
        );
        try {
            VoucherBatchUploadResponse body = batchUploadService.uploadBatch(
                tenantId, programmeUid, catalogRewardUid, file, partnerUid
            );
            return ResponseEntity.accepted().body(body);
        } catch (IllegalArgumentException | VoucherBatchUploadException | VoucherCatalogException e) {
            VoucherBatchUploadResponse err = new VoucherBatchUploadResponse();
            err.setStatus("ERROR");
            err.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/batches/{batchUid}")
    public ResponseEntity<VoucherBatchDetailResponse> getBatch(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String batchUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        VoucherBatch batch = batchRepository.findByBatchUid(batchUid)
            .filter(b -> tenantId.equals(b.getTenantId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));
        return ResponseEntity.ok(toDetail(batch));
    }

    @GetMapping("/batches")
    public ResponseEntity<List<VoucherBatchListDto>> listBatches(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String programmeUid,
        @RequestParam(required = false) String catalogRewardUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String programme = programmeUid == null || programmeUid.isBlank() ? null : programmeUid.trim();
        String catalog = catalogRewardUid == null || catalogRewardUid.isBlank() ? null : catalogRewardUid.trim();

        List<VoucherBatch> batches;
        if (programme != null && catalog != null) {
            batches = batchRepository.findByTenantIdAndProgrammeUidAndCatalogRewardUidOrderByUploadedAtDesc(
                tenantId, programme, catalog
            );
        } else if (programme != null) {
            batches = batchRepository.findByTenantIdAndProgrammeUidOrderByUploadedAtDesc(tenantId, programme);
        } else {
            batches = batchRepository.findByTenantIdOrderByUploadedAtDesc(tenantId);
        }

        List<VoucherBatchListDto> list = batches.stream().map(this::toListDto).toList();
        return ResponseEntity.ok(list);
    }

    private VoucherBatchListDto toListDto(VoucherBatch b) {
        VoucherBatchListDto dto = new VoucherBatchListDto();
        dto.setBatchUid(b.getBatchUid());
        dto.setProgrammeUid(b.getProgrammeUid());
        dto.setStatus(b.getStatus().name());
        dto.setCatalogRewardUid(b.getCatalogRewardUid());
        dto.setOriginalFilename(b.getOriginalFilename());
        dto.setTotalRowsUploaded(b.getTotalRowsUploaded());
        dto.setImportedCount(b.getImportedCount());
        dto.setDuplicateCount(b.getDuplicateCount());
        dto.setErrorCount(b.getErrorCount());
        dto.setUploadedAt(b.getUploadedAt());
        return dto;
    }

    @GetMapping("/stock/{catalogRewardUid}")
    public ResponseEntity<VoucherStockDto> stock(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String catalogRewardUid,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(inventoryService.stock(tenantId, programmeUid, catalogRewardUid));
    }

    private VoucherBatchDetailResponse toDetail(VoucherBatch batch) {
        VoucherBatchDetailResponse dto = new VoucherBatchDetailResponse();
        dto.setBatchUid(batch.getBatchUid());
        dto.setStatus(batch.getStatus().name());
        dto.setTotalRowsUploaded(batch.getTotalRowsUploaded());
        dto.setImportedCount(batch.getImportedCount());
        dto.setDuplicateCount(batch.getDuplicateCount());
        dto.setErrorCount(batch.getErrorCount());
        dto.setExpiredCount(batch.getExpiredCount());
        dto.setUploadedAt(batch.getUploadedAt());
        dto.setCompletedAt(batch.getCompletedAt());
        dto.setUploadedBy(batch.getUploadedBy());
        if (batch.getErrorReportJson() != null) {
            try {
                dto.setErrorReport(objectMapper.readValue(
                    batch.getErrorReportJson(),
                    new TypeReference<List<Map<String, Object>>>() {}
                ));
            } catch (Exception e) {
                log.warn("Could not parse error report for batch {}", batch.getBatchUid());
            }
        }
        return dto;
    }
}
