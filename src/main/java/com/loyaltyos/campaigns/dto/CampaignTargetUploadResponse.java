package com.loyaltyos.campaigns.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class CampaignTargetUploadResponse {

    private String uploadUid;
    private String status;
    private Integer totalRowsUploaded;
    private Integer importedCount;
    private Integer duplicateCount;
    private Integer errorCount;
    private List<Map<String, Object>> errorReport;
    /** Tenant that owns this upload (DB tenant_id). */
    private String tenantId;
    /** Same as tenantId — exposed for UI column "Uploaded by". */
    private String uploadedBy;
    private Instant uploadedAt;
    private Instant completedAt;
    private String errorMessage;
    /** True when the same file (SHA-256) was already uploaded for this campaign. */
    private boolean duplicateFileReplay;

    public String getUploadUid() { return uploadUid; }
    public void setUploadUid(String uploadUid) { this.uploadUid = uploadUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalRowsUploaded() { return totalRowsUploaded; }
    public void setTotalRowsUploaded(Integer totalRowsUploaded) { this.totalRowsUploaded = totalRowsUploaded; }
    public Integer getImportedCount() { return importedCount; }
    public void setImportedCount(Integer importedCount) { this.importedCount = importedCount; }
    public Integer getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(Integer duplicateCount) { this.duplicateCount = duplicateCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public List<Map<String, Object>> getErrorReport() { return errorReport; }
    public void setErrorReport(List<Map<String, Object>> errorReport) { this.errorReport = errorReport; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isDuplicateFileReplay() { return duplicateFileReplay; }
    public void setDuplicateFileReplay(boolean duplicateFileReplay) { this.duplicateFileReplay = duplicateFileReplay; }
}
