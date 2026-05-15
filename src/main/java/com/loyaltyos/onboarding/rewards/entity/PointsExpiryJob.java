package com.loyaltyos.onboarding.rewards.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "points_expiry_jobs",
    indexes = {
        @Index(name = "idx_pej_tenant_date", columnList = "tenant_id,batch_date"),
        @Index(name = "idx_pej_status", columnList = "status")
    }
)
public class PointsExpiryJob {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "job_uid", nullable = false, length = 128, unique = true)
    private String jobUid;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "total_expired")
    private Long totalExpired;

    @Column(name = "customers_affected")
    private Long customersAffected;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "status", nullable = false, length = 32)
    private String status = STATUS_SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PointsExpiryJob() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getJobUid() {
        return jobUid;
    }

    public void setJobUid(String jobUid) {
        this.jobUid = jobUid;
    }

    public LocalDate getBatchDate() {
        return batchDate;
    }

    public void setBatchDate(LocalDate batchDate) {
        this.batchDate = batchDate;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Long getTotalExpired() {
        return totalExpired;
    }

    public void setTotalExpired(Long totalExpired) {
        this.totalExpired = totalExpired;
    }

    public Long getCustomersAffected() {
        return customersAffected;
    }

    public void setCustomersAffected(Long customersAffected) {
        this.customersAffected = customersAffected;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
