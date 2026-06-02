package com.loyaltyos.voucher.dto;

import java.time.Instant;

public class VoucherBatchListDto {

    private String batchUid;
    private String status;
    private String catalogRewardUid;
    private Integer importedCount;
    private Instant uploadedAt;

    public String getBatchUid() { return batchUid; }
    public void setBatchUid(String batchUid) { this.batchUid = batchUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public Integer getImportedCount() { return importedCount; }
    public void setImportedCount(Integer importedCount) { this.importedCount = importedCount; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
