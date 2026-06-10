package com.loyaltyos.analytics.dto;

public class ExpiryJobRunRow {

    private String batchDate;
    private String status;
    private Long totalExpired;
    private Long customersAffected;
    private String executedAt;

    public ExpiryJobRunRow() {}

    public ExpiryJobRunRow(String batchDate, String status, Long totalExpired, Long customersAffected, String executedAt) {
        this.batchDate = batchDate;
        this.status = status;
        this.totalExpired = totalExpired;
        this.customersAffected = customersAffected;
        this.executedAt = executedAt;
    }

    public String getBatchDate() { return batchDate; }
    public void setBatchDate(String batchDate) { this.batchDate = batchDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTotalExpired() { return totalExpired; }
    public void setTotalExpired(Long totalExpired) { this.totalExpired = totalExpired; }
    public Long getCustomersAffected() { return customersAffected; }
    public void setCustomersAffected(Long customersAffected) { this.customersAffected = customersAffected; }
    public String getExecutedAt() { return executedAt; }
    public void setExecutedAt(String executedAt) { this.executedAt = executedAt; }
}
