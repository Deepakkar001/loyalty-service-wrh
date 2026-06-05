package com.loyaltyos.support.entity;

import com.loyaltyos.support.enums.SupportActorType;
import com.loyaltyos.support.enums.SupportCaseCategory;
import com.loyaltyos.support.enums.SupportCasePriority;
import com.loyaltyos.support.enums.SupportCaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "support_cases")
public class SupportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "case_uid", nullable = false, unique = true, length = 64)
    private String caseUid;

    @Column(name = "created_by_user_id", nullable = false, length = 128)
    private String createdByUserId;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private SupportCaseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private SupportCasePriority priority = SupportCasePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SupportCaseStatus status = SupportCaseStatus.OPEN;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "programme_uid", length = 64)
    private String programmeUid;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "context_json", columnDefinition = "json")
    private String contextJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    @Column(name = "status_updated_by", length = 255)
    private String statusUpdatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_updated_by_type", length = 20)
    private SupportActorType statusUpdatedByType;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCaseUid() {
        return caseUid;
    }

    public void setCaseUid(String caseUid) {
        this.caseUid = caseUid;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public SupportCaseCategory getCategory() {
        return category;
    }

    public void setCategory(SupportCaseCategory category) {
        this.category = category;
    }

    public SupportCasePriority getPriority() {
        return priority;
    }

    public void setPriority(SupportCasePriority priority) {
        this.priority = priority;
    }

    public SupportCaseStatus getStatus() {
        return status;
    }

    public void setStatus(SupportCaseStatus status) {
        this.status = status;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(Instant statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public String getStatusUpdatedBy() {
        return statusUpdatedBy;
    }

    public void setStatusUpdatedBy(String statusUpdatedBy) {
        this.statusUpdatedBy = statusUpdatedBy;
    }

    public SupportActorType getStatusUpdatedByType() {
        return statusUpdatedByType;
    }

    public void setStatusUpdatedByType(SupportActorType statusUpdatedByType) {
        this.statusUpdatedByType = statusUpdatedByType;
    }
}
