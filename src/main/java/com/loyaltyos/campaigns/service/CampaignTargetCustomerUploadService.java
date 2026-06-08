package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.entity.CampaignTargetCustomer;
import com.loyaltyos.campaigns.entity.CampaignTargetUpload;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CampaignTargetUploadStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetCustomerRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetUploadRepository;
import com.loyaltyos.campaigns.support.CampaignTargetUploadSpec;
import com.loyaltyos.campaigns.support.InMemoryCsvMultipartFile;
import com.loyaltyos.voucher.support.VoucherCodeNormalizer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CampaignTargetCustomerUploadService {

    private static final Logger log = LoggerFactory.getLogger(CampaignTargetCustomerUploadService.class);
    private static final int INSERT_BATCH_SIZE = 500;
    private static final String INSERT_IGNORE_SQL =
        "INSERT IGNORE INTO campaign_target_customers "
            + "(tenant_id, campaign_uid, customer_id, added_by, source_upload_uid, added_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private final CampaignRepository campaignRepository;
    private final CampaignTargetCustomerRepository targetCustomerRepository;
    private final CampaignTargetUploadRepository uploadRepository;
    private final CampaignProperties campaignProperties;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CampaignTargetCustomerUploadService(
        CampaignRepository campaignRepository,
        CampaignTargetCustomerRepository targetCustomerRepository,
        CampaignTargetUploadRepository uploadRepository,
        CampaignProperties campaignProperties,
        ObjectMapper objectMapper,
        JdbcTemplate jdbcTemplate,
        PlatformTransactionManager transactionManager
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.targetCustomerRepository = Objects.requireNonNull(targetCustomerRepository, "targetCustomerRepository");
        this.uploadRepository = Objects.requireNonNull(uploadRepository, "uploadRepository");
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.transactionTemplate = new TransactionTemplate(
            Objects.requireNonNull(transactionManager, "transactionManager")
        );
        // Fail fast instead of holding a connection until the HTTP client times out.
        this.transactionTemplate.setTimeout(90);
    }

    /**
     * Parses and validates the upload outside a DB transaction so file I/O does not hold a connection.
     */
    public CampaignTargetUploadResponse uploadCsv(String tenantId, String campaignUid, MultipartFile file) {
        return uploadCsv(tenantId, campaignUid, file, currentActor());
    }

    /**
     * @param uploadedBy audit actor (portal user id, or e.g. {@code integration:keyUid})
     */
    public CampaignTargetUploadResponse uploadCsv(
        String tenantId,
        String campaignUid,
        MultipartFile file,
        String uploadedBy
    ) {
        log.info(
            "Campaign target upload starting tenant={} campaign={} filename={} sizeBytes={}",
            tenantId,
            campaignUid,
            file.getOriginalFilename(),
            file.getSize()
        );
        validateFile(file);
        CampaignProperties.TargetCustomerUpload cfg = campaignProperties.getTargetCustomerUpload();

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new CampaignBadRequestException("Failed to read upload file");
        }
        log.info(
            "Campaign target upload read bytes tenant={} campaign={} bytes={}",
            tenantId,
            campaignUid,
            fileBytes.length
        );

        String fileSha256 = VoucherCodeNormalizer.sha256Hex(fileBytes);
        log.info(
            "Campaign target upload checksum tenant={} campaign={} shaPrefix={}",
            tenantId,
            campaignUid,
            fileSha256.substring(0, 12)
        );
        List<String> customerIds;
        try {
            customerIds = parseCustomerIds(new ByteArrayInputStream(fileBytes), cfg);
        } catch (IOException e) {
            throw new CampaignBadRequestException("Failed to parse CSV");
        } catch (IllegalArgumentException e) {
            log.warn(
                "Campaign target upload CSV validation failed tenant={} campaign={}: {}",
                tenantId,
                campaignUid,
                e.getMessage()
            );
            throw e;
        }

        log.info(
            "Campaign target upload parsed tenant={} campaign={} rowCount={} sha={}",
            tenantId,
            campaignUid,
            customerIds.size(),
            fileSha256.substring(0, 12) + "..."
        );

        if (customerIds.size() > cfg.getMaxRows()) {
            throw new IllegalArgumentException("File exceeds max rows: " + cfg.getMaxRows());
        }

        String actor = normalizeActor(uploadedBy);
        CampaignTargetUploadResponse response = transactionTemplate.execute(
            status -> persistUpload(tenantId, campaignUid, file, fileSha256, customerIds, cfg, actor)
        );
        if (response == null) {
            throw new CampaignBadRequestException("Upload transaction did not complete");
        }
        return response;
    }

    /**
     * Appends customer IDs via the same validation and persistence path as CSV upload.
     */
    public CampaignTargetUploadResponse uploadCustomerIds(
        String tenantId,
        String campaignUid,
        List<String> customerIds,
        String uploadedBy
    ) {
        if (customerIds == null || customerIds.isEmpty()) {
            throw new CampaignBadRequestException("customerIds must not be empty");
        }
        StringBuilder csv = new StringBuilder("customer_id\n");
        for (String id : customerIds) {
            if (id != null && !id.isBlank()) {
                csv.append(id.trim()).append('\n');
            }
        }
        MultipartFile file = new InMemoryCsvMultipartFile(
            "integration-bulk.csv",
            csv.toString().getBytes(StandardCharsets.UTF_8)
        );
        return uploadCsv(tenantId, campaignUid, file, uploadedBy);
    }

    private CampaignTargetUploadResponse persistUpload(
        String tenantId,
        String campaignUid,
        MultipartFile file,
        String fileSha256,
        List<String> customerIds,
        CampaignProperties.TargetCustomerUpload cfg,
        String actor
    ) {
        log.info("Campaign target upload persisting tenant={} campaign={}", tenantId, campaignUid);
        Campaign campaign = loadCampaign(tenantId, campaignUid);
        assertUploadAllowed(campaign);

        Optional<CampaignTargetUpload> existingUpload =
            uploadRepository.findTopByTenantIdAndCampaignUidAndFileSha256OrderByUploadedAtDesc(
                tenantId,
                campaignUid,
                fileSha256
            );
        if (existingUpload.isPresent()) {
            log.info(
                "Duplicate campaign target upload for tenant {} campaign {} sha {} uploadUid={}",
                tenantId,
                campaignUid,
                fileSha256,
                existingUpload.get().getUploadUid()
            );
            refreshCustomerCount(campaign);
            campaignRepository.save(campaign);
            CampaignTargetUploadResponse replay = toUploadResponse(existingUpload.get(), null);
            replay.setDuplicateFileReplay(true);
            return replay;
        }

        String uploadUid = UUID.randomUUID().toString();
        CampaignTargetUpload batch = new CampaignTargetUpload();
        batch.setUploadUid(uploadUid);
        batch.setTenantId(tenantId);
        batch.setCampaignUid(campaignUid);
        batch.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv");
        batch.setFileSizeBytes(file.getSize());
        batch.setFileSha256(fileSha256);
        batch.setStatus(CampaignTargetUploadStatus.PROCESSING);
        batch.setUploadedBy(actor);
        uploadRepository.save(batch);

        batch.setTotalRowsUploaded(customerIds.size());
        List<Map<String, Object>> errors = new ArrayList<>();
        List<CampaignTargetCustomer> toInsert = new ArrayList<>();
        Set<String> seenInThisFile = new HashSet<>();
        int fileDuplicateCount = 0;

        for (int i = 0; i < customerIds.size(); i++) {
            int rowNum = i + 2;
            String customerId = customerIds.get(i);
            List<String> rowErrors = validateCustomerId(customerId, cfg);
            if (!rowErrors.isEmpty()) {
                for (String reason : rowErrors) {
                    errors.add(errorRow(rowNum, customerId, reason));
                }
                batch.setErrorCount(batch.getErrorCount() + 1);
                continue;
            }

            if (!seenInThisFile.add(customerId)) {
                fileDuplicateCount++;
                continue;
            }

            CampaignTargetCustomer row = new CampaignTargetCustomer();
            row.setCustomerId(customerId);
            toInsert.add(row);
        }

        long countBefore = targetCustomerRepository.countByTenantIdAndCampaignUid(tenantId, campaignUid);
        if (!toInsert.isEmpty()) {
            log.info(
                "Campaign target upload inserting tenant={} campaign={} candidates={} countBefore={}",
                tenantId,
                campaignUid,
                toInsert.size(),
                countBefore
            );
            insertIgnoreBatch(tenantId, campaignUid, toInsert, actor, uploadUid);
        }
        long countAfter = targetCustomerRepository.countByTenantIdAndCampaignUid(tenantId, campaignUid);
        log.info(
            "Campaign target upload inserted tenant={} campaign={} countBefore={} countAfter={}",
            tenantId,
            campaignUid,
            countBefore,
            countAfter
        );
        int actuallyInserted = (int) Math.min(Math.max(0, countAfter - countBefore), Integer.MAX_VALUE);
        int dbDuplicateCount = toInsert.size() - actuallyInserted;

        batch.setImportedCount(actuallyInserted);
        batch.setDuplicateCount(fileDuplicateCount + dbDuplicateCount);

        campaign.setCustomerScope(CustomerScope.TARGETED);
        campaign.setCustomerCount(Math.toIntExact(Math.min(countAfter, Integer.MAX_VALUE)));
        campaignRepository.save(campaign);

        batch.setStatus(CampaignTargetUploadStatus.COMPLETED);
        batch.setCompletedAt(Instant.now());
        if (!errors.isEmpty()) {
            try {
                batch.setErrorReportJson(objectMapper.writeValueAsString(errors));
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize campaign target upload error report {}", uploadUid, e);
            }
        }
        uploadRepository.save(batch);

        log.info(
            "Campaign target upload {} completed: imported={}, duplicates={}, errors={}",
            uploadUid,
            batch.getImportedCount(),
            batch.getDuplicateCount(),
            batch.getErrorCount()
        );

        return toUploadResponse(batch, errors.isEmpty() ? null : errors);
    }

    private void insertIgnoreBatch(
        String tenantId,
        String campaignUid,
        List<CampaignTargetCustomer> rows,
        String actor,
        String uploadUid
    ) {
        Timestamp addedAt = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate(
            INSERT_IGNORE_SQL,
            rows,
            INSERT_BATCH_SIZE,
            (ps, row) -> {
                ps.setString(1, tenantId);
                ps.setString(2, campaignUid);
                ps.setString(3, row.getCustomerId());
                ps.setString(4, actor);
                ps.setString(5, uploadUid);
                ps.setTimestamp(6, addedAt);
            }
        );
    }

    private void refreshCustomerCount(Campaign campaign) {
        long count = targetCustomerRepository.countByTenantIdAndCampaignUid(
            campaign.getTenantId(),
            campaign.getCampaignUid()
        );
        campaign.setCustomerCount(Math.toIntExact(Math.min(count, Integer.MAX_VALUE)));
    }

    private static void assertUploadAllowed(Campaign campaign) {
        if (campaign.getStatus() == CampaignStatus.ENDED
            || campaign.getStatus() == CampaignStatus.EXHAUSTED
            || campaign.getStatus() == CampaignStatus.EXPIRED) {
            throw new CampaignConflictException(
                "Cannot upload customer list for campaign in status " + campaign.getStatus()
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        int maxMb = campaignProperties.getTargetCustomerUpload().getMaxFileSizeMb();
        long maxBytes = (long) maxMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File exceeds " + maxMb + " MB limit");
        }
        String name = file.getOriginalFilename();
        if (name != null && !name.isBlank()) {
            String lower = name.toLowerCase();
            if (!lower.endsWith(".csv")) {
                throw new IllegalArgumentException("Only CSV files are supported");
            }
        }
    }

    /**
     * Single-column CSV parser (header {@code customer_id} + one ID per line).
     * Avoids Apache Commons CSV, which can block indefinitely on some JVM/IDE setups.
     */
    private List<String> parseCustomerIds(
        java.io.InputStream stream,
        CampaignProperties.TargetCustomerUpload cfg
    ) throws IOException {
        List<String> rows = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int fileDuplicateCount = 0;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV is empty");
            }
            validateHeaderLine(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String customerId = firstCsvField(line);
                if (customerId.isBlank()) {
                    continue;
                }
                if (!seenInFile.add(customerId)) {
                    fileDuplicateCount++;
                    continue;
                }
                rows.add(customerId);
            }
        }

        if (fileDuplicateCount > 0) {
            log.debug("Ignored {} duplicate customer_id value(s) within upload file", fileDuplicateCount);
        }
        return rows;
    }

    private static void validateHeaderLine(String headerLine) {
        String header = normalizeHeaderName(firstCsvField(headerLine));
        if (!CampaignTargetUploadSpec.STANDARD_HEADERS.contains(header)) {
            throw new IllegalArgumentException(
                "CSV is missing required column(s): customer_id. "
                    + "File must include header: "
                    + String.join(", ", CampaignTargetUploadSpec.STANDARD_HEADERS)
            );
        }
    }

    /** First column of a CSV line (supports optional UTF-8 BOM and simple quotes). */
    private static String firstCsvField(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1).trim();
        }
        int comma = trimmed.indexOf(',');
        String field = comma < 0 ? trimmed : trimmed.substring(0, comma).trim();
        if (field.length() >= 2 && field.startsWith("\"") && field.endsWith("\"")) {
            field = field.substring(1, field.length() - 1).trim();
        }
        return field;
    }

    private static List<String> validateCustomerId(
        String customerId,
        CampaignProperties.TargetCustomerUpload cfg
    ) {
        List<String> errors = new ArrayList<>();
        if (customerId == null || customerId.isBlank()) {
            errors.add("customer_id is required");
            return errors;
        }
        if (customerId.length() < cfg.getMinCustomerIdLength()) {
            errors.add("customer_id is too short");
        }
        if (customerId.length() > cfg.getMaxCustomerIdLength()) {
            errors.add("customer_id exceeds max length (" + cfg.getMaxCustomerIdLength() + ")");
        }
        return errors;
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

    private static Map<String, Object> errorRow(int row, String customerId, String reason) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("row", row);
        map.put("customerId", customerId != null ? customerId : "N/A");
        map.put("reason", reason);
        return map;
    }

    private Campaign loadCampaign(String tenantId, String campaignUid) {
        return campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));
    }

    private CampaignTargetUploadResponse toUploadResponse(
        CampaignTargetUpload batch,
        List<Map<String, Object>> errors
    ) {
        CampaignTargetUploadResponse response = new CampaignTargetUploadResponse();
        response.setUploadUid(batch.getUploadUid());
        response.setStatus(batch.getStatus().name());
        response.setTotalRowsUploaded(batch.getTotalRowsUploaded());
        response.setImportedCount(batch.getImportedCount());
        response.setDuplicateCount(batch.getDuplicateCount());
        response.setErrorCount(batch.getErrorCount());
        response.setTenantId(batch.getTenantId());
        response.setUploadedBy(batch.getTenantId());
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
                log.warn("Could not parse stored error report for upload {}", batch.getUploadUid());
            }
        }
        return response;
    }

    private static String currentActor() {
        var context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() != null) {
            return context.getAuthentication().getName();
        }
        return "SYSTEM";
    }

    private static String normalizeActor(String uploadedBy) {
        if (uploadedBy != null && !uploadedBy.isBlank()) {
            return uploadedBy.trim();
        }
        return currentActor();
    }
}

