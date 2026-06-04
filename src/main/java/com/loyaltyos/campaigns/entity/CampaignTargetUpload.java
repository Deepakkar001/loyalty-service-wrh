package com.loyaltyos.campaigns.entity;

import com.loyaltyos.campaigns.enums.CampaignTargetUploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "campaign_target_uploads",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_upload_uid",
        columnNames = {"tenant_id", "upload_uid"}
    ),
    indexes = {
        @Index(name = "idx_tenant_campaign", columnList = "tenant_id,campaign_uid"),
        @Index(name = "idx_tenant_file_sha", columnList = "tenant_id,file_sha256")
    }
)
public class CampaignTargetUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "campaign_uid", nullable = false, length = 128)
    private String campaignUid;

    @Column(name = "upload_uid", nullable = false, length = 128)
    private String uploadUid;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_sha256", length = 64)
    private String fileSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CampaignTargetUploadStatus status = CampaignTargetUploadStatus.PROCESSING;

    @Column(name = "total_rows_uploaded", nullable = false)
    private Integer totalRowsUploaded = 0;

    @Column(name = "imported_count", nullable = false)
    private Integer importedCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_report_json", columnDefinition = "JSON")
    private String errorReportJson;

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getUploadUid() { return uploadUid; }
    public void setUploadUid(String uploadUid) { this.uploadUid = uploadUid; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }
    public CampaignTargetUploadStatus getStatus() { return status; }
    public void setStatus(CampaignTargetUploadStatus status) { this.status = status; }
    public Integer getTotalRowsUploaded() { return totalRowsUploaded; }
    public void setTotalRowsUploaded(Integer totalRowsUploaded) { this.totalRowsUploaded = totalRowsUploaded; }
    public Integer getImportedCount() { return importedCount; }
    public void setImportedCount(Integer importedCount) { this.importedCount = importedCount; }
    public Integer getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(Integer duplicateCount) { this.duplicateCount = duplicateCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public String getErrorReportJson() { return errorReportJson; }
    public void setErrorReportJson(String errorReportJson) { this.errorReportJson = errorReportJson; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
