package com.loyaltyos.voucher.entity;

import com.loyaltyos.voucher.enums.VoucherBatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "voucher_batch",
    indexes = {
        @Index(name = "idx_tenant_programme", columnList = "tenant_id,programme_uid"),
        @Index(name = "idx_tenant_catalog", columnList = "tenant_id,catalog_reward_uid"),
        @Index(name = "idx_tenant_file_sha", columnList = "tenant_id,file_sha256")
    }
)
public class VoucherBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_uid", nullable = false, unique = true, length = 128)
    private String batchUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "catalog_reward_uid", nullable = false, length = 64)
    private String catalogRewardUid;

    @Column(name = "partner_uid", length = 128)
    private String partnerUid;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_sha256", length = 64)
    private String fileSha256;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_mapping_json", columnDefinition = "JSON")
    private String columnMappingJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VoucherBatchStatus status = VoucherBatchStatus.PROCESSING;

    @Column(name = "total_rows_uploaded", nullable = false)
    private int totalRowsUploaded;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Column(name = "expired_count", nullable = false)
    private int expiredCount;

    @Column(name = "invalid_count", nullable = false)
    private int invalidCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_report_json", columnDefinition = "JSON")
    private String errorReportJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @Column(name = "uploaded_by", nullable = false, length = 255)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public VoucherBatch() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchUid() { return batchUid; }
    public void setBatchUid(String batchUid) { this.batchUid = batchUid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public String getPartnerUid() { return partnerUid; }
    public void setPartnerUid(String partnerUid) { this.partnerUid = partnerUid; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }
    public String getColumnMappingJson() { return columnMappingJson; }
    public void setColumnMappingJson(String columnMappingJson) { this.columnMappingJson = columnMappingJson; }
    public VoucherBatchStatus getStatus() { return status; }
    public void setStatus(VoucherBatchStatus status) { this.status = status; }
    public int getTotalRowsUploaded() { return totalRowsUploaded; }
    public void setTotalRowsUploaded(int totalRowsUploaded) { this.totalRowsUploaded = totalRowsUploaded; }
    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
    public int getExpiredCount() { return expiredCount; }
    public void setExpiredCount(int expiredCount) { this.expiredCount = expiredCount; }
    public int getInvalidCount() { return invalidCount; }
    public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public String getErrorReportJson() { return errorReportJson; }
    public void setErrorReportJson(String errorReportJson) { this.errorReportJson = errorReportJson; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
