package com.loyaltyos.voucher.dto;

import java.time.Instant;

public class VoucherBatchListDto {

    private String batchUid;
    private String programmeUid;
    private String status;
    private String catalogRewardUid;
    private String originalFilename;
    private Integer totalRowsUploaded;
    private Integer importedCount;
    private Integer duplicateCount;
    private Integer errorCount;
    private Instant uploadedAt;

    public String getBatchUid() { return batchUid; }
    public void setBatchUid(String batchUid) { this.batchUid = batchUid; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Integer getTotalRowsUploaded() { return totalRowsUploaded; }
    public void setTotalRowsUploaded(Integer totalRowsUploaded) { this.totalRowsUploaded = totalRowsUploaded; }
    public Integer getImportedCount() { return importedCount; }
    public void setImportedCount(Integer importedCount) { this.importedCount = importedCount; }
    public Integer getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(Integer duplicateCount) { this.duplicateCount = duplicateCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
