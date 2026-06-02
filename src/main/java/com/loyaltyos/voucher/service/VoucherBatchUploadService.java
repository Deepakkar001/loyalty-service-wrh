package com.loyaltyos.voucher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherBatchUploadResponse;
import com.loyaltyos.voucher.dto.VoucherStockBreakdownDto;
import com.loyaltyos.voucher.dto.VoucherCsvRow;
import com.loyaltyos.voucher.entity.VoucherBatch;
import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import com.loyaltyos.voucher.entity.VoucherInventory;
import com.loyaltyos.voucher.enums.VoucherBatchStatus;
import com.loyaltyos.voucher.enums.VoucherStatus;
import com.loyaltyos.voucher.exception.VoucherBatchUploadException;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.repository.VoucherBatchRepository;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import com.loyaltyos.voucher.support.VoucherCodeNormalizer;
import com.loyaltyos.voucher.support.VoucherDenominationSupport;
import com.loyaltyos.voucher.support.VoucherUploadSpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoucherBatchUploadService {

    private static final Logger log = LoggerFactory.getLogger(VoucherBatchUploadService.class);
    private static final String VOUCHER_TYPE = "VOUCHER";

    private final VoucherBatchRepository batchRepository;
    private final VoucherInventoryRepository inventoryRepository;
    private final VoucherCodeCryptoService cryptoService;
    private final RewardCatalogService rewardCatalogService;
    private final VoucherProperties properties;
    private final ObjectMapper objectMapper;
    private final VoucherDenominationMappingService denominationMappingService;

    public VoucherBatchUploadService(
        VoucherBatchRepository batchRepository,
        VoucherInventoryRepository inventoryRepository,
        VoucherCodeCryptoService cryptoService,
        RewardCatalogService rewardCatalogService,
        VoucherProperties properties,
        ObjectMapper objectMapper,
        VoucherDenominationMappingService denominationMappingService
    ) {
        this.batchRepository = Objects.requireNonNull(batchRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.cryptoService = Objects.requireNonNull(cryptoService);
        this.rewardCatalogService = Objects.requireNonNull(rewardCatalogService);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.denominationMappingService = Objects.requireNonNull(denominationMappingService);
    }

    @Transactional
    public VoucherBatchUploadResponse uploadBatch(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        MultipartFile file,
        String partnerUid
    ) {
        validateUploadInput(tenantId, programmeUid, catalogRewardUid, file);
        String programme = normalizeProgramme(programmeUid);
        String catalogUid = catalogRewardUid.trim();

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new VoucherBatchUploadException("Failed to read upload file", e);
        }

        String fileSha256 = VoucherCodeNormalizer.sha256Hex(fileBytes);
        Optional<VoucherBatch> existing = batchRepository.findByTenantIdAndFileSha256(tenantId, fileSha256);
        if (existing.isPresent()) {
            log.info("Duplicate voucher batch upload for tenant {} sha {}", tenantId, fileSha256);
            return toUploadResponse(existing.get(), null);
        }

        String batchUid = UUID.randomUUID().toString();
        VoucherBatch batch = new VoucherBatch();
        batch.setBatchUid(batchUid);
        batch.setTenantId(tenantId);
        batch.setProgrammeUid(programme);
        batch.setCatalogRewardUid(catalogUid);
        batch.setPartnerUid(partnerUid);
        batch.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv");
        batch.setFileSizeBytes(file.getSize());
        batch.setFileSha256(fileSha256);
        batch.setStatus(VoucherBatchStatus.PROCESSING);
        batch.setUploadedBy(currentActor());
        batchRepository.save(batch);

        List<VoucherCsvRow> rows;
        try {
            rows = parseCsv(new ByteArrayInputStream(fileBytes));
        } catch (IOException e) {
            batch.setStatus(VoucherBatchStatus.FAILED);
            batchRepository.save(batch);
            throw new VoucherBatchUploadException("Failed to parse CSV", e);
        } catch (IllegalArgumentException e) {
            batch.setStatus(VoucherBatchStatus.FAILED);
            try {
                batch.setErrorReportJson(objectMapper.writeValueAsString(
                    List.of(Map.of("row", 0, "code", "N/A", "reason", e.getMessage()))
                ));
            } catch (JsonProcessingException ignored) {
                // ignore
            }
            batchRepository.save(batch);
            throw e;
        }

        if (rows.size() > properties.getMaxRows()) {
            batch.setStatus(VoucherBatchStatus.FAILED);
            batchRepository.save(batch);
            throw new IllegalArgumentException("File exceeds max rows: " + properties.getMaxRows());
        }

        batch.setTotalRowsUploaded(rows.size());
        List<Map<String, Object>> errors = new ArrayList<>();
        List<VoucherInventory> toInsert = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        Map<BigDecimal, VoucherDenominationMapping> faceValueMappings =
            denominationMappingService.faceValueIndex(tenantId, catalogUid);
        boolean mixedDenomination = !faceValueMappings.isEmpty();
        if (mixedDenomination) {
            log.info("Mixed-denomination upload for catalog {} ({} tiers)", catalogUid, faceValueMappings.size());
        }
        Map<String, Integer> stockByDenomination = new LinkedHashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            VoucherCsvRow row = rows.get(i);
            int rowNum = i + 2;
            try {
                List<String> rowErrors = validateCsvRow(row);
                if (!rowErrors.isEmpty()) {
                    errors.add(errorRow(rowNum, row.getCode(), String.join("; ", rowErrors)));
                    batch.setErrorCount(batch.getErrorCount() + 1);
                    continue;
                }

                String normalized = VoucherCodeNormalizer.normalize(row.getCode());
                if (!seenInFile.add(normalized)) {
                    errors.add(errorRow(rowNum, row.getCode(), "Duplicate code in file"));
                    batch.setDuplicateCount(batch.getDuplicateCount() + 1);
                    continue;
                }

                String codeHash = VoucherCodeNormalizer.sha256Hex(normalized);
                if (inventoryRepository.existsByTenantIdAndCodeHash(tenantId, codeHash)) {
                    errors.add(errorRow(rowNum, row.getCode(), "Duplicate code already in inventory"));
                    batch.setDuplicateCount(batch.getDuplicateCount() + 1);
                    continue;
                }

                BigDecimal rowFace = VoucherDenominationSupport.normalizeAmount(row.getFaceValue());
                VoucherDenominationMapping denominationMapping = null;
                if (mixedDenomination) {
                    denominationMapping = faceValueMappings.get(rowFace);
                    if (denominationMapping == null) {
                        errors.add(errorRow(
                            rowNum,
                            row.getCode(),
                            "face_value " + rowFace.toPlainString() + " is not in denomination mappings"
                        ));
                        batch.setErrorCount(batch.getErrorCount() + 1);
                        continue;
                    }
                }

                VoucherInventory inventory = new VoucherInventory();
                inventory.setInventoryUid(UUID.randomUUID().toString());
                inventory.setTenantId(tenantId);
                inventory.setProgrammeUid(programme);
                inventory.setCatalogRewardUid(catalogUid);
                if (denominationMapping != null) {
                    inventory.setDenominationMappingId(denominationMapping.getId());
                }
                inventory.setBatchUid(batchUid);
                inventory.setCodeHash(codeHash);
                inventory.setCodeCiphertext(cryptoService.encryptCode(normalized));
                if (row.getPin() != null && !row.getPin().isBlank()) {
                    String pin = row.getPin().trim();
                    inventory.setPinHash(VoucherCodeNormalizer.sha256Hex(pin));
                    inventory.setPinCiphertext(cryptoService.encryptCode(pin));
                }
                inventory.setFaceValue(row.getFaceValue());
                inventory.setCurrency(row.getCurrency());
                inventory.setExpiresAt(row.getExpiresAt());
                inventory.setPartnerSku(row.getPartnerSku());
                inventory.setStatus(VoucherStatus.AVAILABLE);

                Map<String, Object> extra = new LinkedHashMap<>();
                if (row.getSerial() != null && !row.getSerial().isBlank()) {
                    extra.put("serial", row.getSerial().trim());
                }
                if (row.getRegion() != null && !row.getRegion().isBlank()) {
                    extra.put("region", row.getRegion().trim());
                }
                if (row.getChannel() != null && !row.getChannel().isBlank()) {
                    extra.put("channel", row.getChannel().trim());
                }
                if (row.getExternalRef() != null && !row.getExternalRef().isBlank()) {
                    extra.put("external_ref", row.getExternalRef().trim());
                }
                if (!extra.isEmpty()) {
                    inventory.setExtraMetadataJson(objectMapper.writeValueAsString(extra));
                }

                toInsert.add(inventory);
                batch.setImportedCount(batch.getImportedCount() + 1);
                if (rowFace != null) {
                    stockByDenomination.merge(VoucherStockBreakdownDto.faceValueKey(rowFace), 1, Integer::sum);
                }
            } catch (Exception e) {
                log.warn("Voucher import row {} failed: {}", rowNum, e.getMessage());
                errors.add(errorRow(rowNum, row.getCode(), "Error: " + e.getMessage()));
                batch.setErrorCount(batch.getErrorCount() + 1);
            }
        }

        if (!toInsert.isEmpty()) {
            inventoryRepository.saveAll(toInsert);
        }

        batch.setStatus(VoucherBatchStatus.COMPLETED);
        batch.setCompletedAt(Instant.now());
        if (!stockByDenomination.isEmpty()) {
            try {
                batch.setMetadataJson(objectMapper.writeValueAsString(
                    Map.of("stockByDenomination", stockByDenomination)
                ));
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize batch metadata", e);
            }
        }
        if (!errors.isEmpty()) {
            try {
                batch.setErrorReportJson(objectMapper.writeValueAsString(errors));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize error report", e);
            }
        }
        batchRepository.save(batch);

        log.info(
            "Voucher batch {} completed: imported={}, errors={}, duplicates={}, expired={}",
            batchUid, batch.getImportedCount(), batch.getErrorCount(),
            batch.getDuplicateCount(), batch.getExpiredCount()
        );

        return toUploadResponse(batch, errors.isEmpty() ? null : errors);
    }

    private void validateUploadInput(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        long maxBytes = (long) properties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File exceeds " + properties.getMaxFileSizeMb() + " MB limit");
        }

        String programme = normalizeProgramme(programmeUid);
        RewardCatalogItem item = rewardCatalogService.findActiveItem(tenantId, programme, catalogRewardUid)
            .orElseThrow(() -> new VoucherCatalogException("Catalog item not found or inactive: " + catalogRewardUid));

        if (!VOUCHER_TYPE.equalsIgnoreCase(item.rewardType())) {
            throw new VoucherCatalogException("Catalog item is not type VOUCHER: " + catalogRewardUid);
        }
    }

    private static List<String> validateCsvRow(VoucherCsvRow row) {
        List<String> errors = new ArrayList<>();
        if (row.getCode() == null || row.getCode().isBlank()) {
            errors.add("code is required");
        } else if (row.getCode().trim().length() < 3) {
            errors.add("code must be at least 3 characters");
        } else if (row.getCode().length() > 512) {
            errors.add("code exceeds max length (512)");
        }
        if (row.getPin() != null && row.getPin().length() > 128) {
            errors.add("pin exceeds max length (128)");
        }
        if (row.getFaceValue() == null) {
            errors.add("face_value is required");
        } else {
            if (row.getFaceValue().signum() <= 0) {
                errors.add("face_value must be positive");
            }
            if (row.getFaceValue().scale() > 4) {
                errors.add("face_value scale exceeds 4 decimal places");
            }
        }
        if (row.getCurrency() == null || row.getCurrency().isBlank()) {
            errors.add("currency is required");
        } else {
            String c = row.getCurrency().trim().toUpperCase(Locale.ROOT);
            if (c.length() != 3) {
                errors.add("currency must be 3-char ISO code");
            } else {
                try {
                    Currency.getInstance(c);
                } catch (IllegalArgumentException e) {
                    errors.add("Invalid currency code: " + c);
                }
            }
        }
        if (row.getExpiresAt() == null) {
            errors.add("expires_at is required (ISO-8601 UTC, e.g. 2026-12-31T23:59:59Z)");
        } else if (row.getExpiresAt().isBefore(Instant.now())) {
            errors.add("expires_at must be in the future");
        }
        return errors;
    }

    private List<VoucherCsvRow> parseCsv(java.io.InputStream stream) throws IOException {
        List<VoucherCsvRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build()
            .parse(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            validateStandardHeaders(parser.getHeaderMap().keySet());

            for (CSVRecord record : parser) {
                VoucherCsvRow row = new VoucherCsvRow();
                row.setCode(record.get("code"));
                if (record.isMapped("pin")) {
                    row.setPin(record.get("pin"));
                }
                if (record.isMapped("face_value") && !record.get("face_value").isBlank()) {
                    row.setFaceValue(new BigDecimal(record.get("face_value").trim()));
                }
                if (record.isMapped("currency")) {
                    row.setCurrency(record.get("currency"));
                }
                if (record.isMapped("expires_at") && !record.get("expires_at").isBlank()) {
                    row.setExpiresAt(Instant.parse(record.get("expires_at").trim()));
                }
                if (record.isMapped("partner_sku")) {
                    row.setPartnerSku(record.get("partner_sku"));
                }
                if (record.isMapped("serial")) {
                    row.setSerial(record.get("serial"));
                }
                if (record.isMapped("region")) {
                    row.setRegion(record.get("region"));
                }
                if (record.isMapped("channel")) {
                    row.setChannel(record.get("channel"));
                }
                if (record.isMapped("external_ref")) {
                    row.setExternalRef(record.get("external_ref"));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static void validateStandardHeaders(Set<String> rawHeaders) {
        Set<String> headers = new HashSet<>();
        for (String h : rawHeaders) {
            headers.add(normalizeHeaderName(h));
        }
        List<String> missing = new ArrayList<>();
        for (String required : VoucherUploadSpec.STANDARD_HEADERS) {
            if (!headers.contains(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "CSV is missing required column(s): "
                    + String.join(", ", missing)
                    + ". File must include header exactly: "
                    + String.join(", ", VoucherUploadSpec.STANDARD_HEADERS)
            );
        }
    }

    private static String normalizeHeaderName(String header) {
        if (header == null) {
            return "";
        }
        String trimmed = header.trim();
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private static Map<String, Object> errorRow(int row, String code, String reason) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("row", row);
        map.put("code", code != null ? code : "N/A");
        map.put("reason", reason);
        return map;
    }

    private VoucherBatchUploadResponse toUploadResponse(VoucherBatch batch, List<Map<String, Object>> errors) {
        VoucherBatchUploadResponse response = new VoucherBatchUploadResponse();
        response.setBatchUid(batch.getBatchUid());
        response.setStatus(batch.getStatus().name());
        response.setTotalRowsUploaded(batch.getTotalRowsUploaded());
        response.setImportedCount(batch.getImportedCount());
        response.setDuplicateCount(batch.getDuplicateCount());
        response.setErrorCount(batch.getErrorCount());
        response.setExpiredCount(batch.getExpiredCount());
        response.setUploadedAt(batch.getUploadedAt());
        response.setCompletedAt(batch.getCompletedAt());
        if (errors != null) {
            response.setErrorReport(errors);
        } else if (batch.getErrorReportJson() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parsed = objectMapper.readValue(
                    batch.getErrorReportJson(),
                    List.class
                );
                response.setErrorReport(parsed);
            } catch (JsonProcessingException e) {
                log.warn("Could not parse stored error report for batch {}", batch.getBatchUid());
            }
        }
        return response;
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }

    private static String currentActor() {
        var context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() != null) {
            return context.getAuthentication().getName();
        }
        return "SYSTEM";
    }
}
