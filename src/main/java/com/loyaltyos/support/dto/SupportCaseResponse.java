package com.loyaltyos.support.dto;

import com.loyaltyos.support.enums.SupportActorType;
import com.loyaltyos.support.enums.SupportCaseCategory;
import com.loyaltyos.support.enums.SupportCasePriority;
import com.loyaltyos.support.enums.SupportCaseStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SupportCaseResponse {

    private String caseUid;
    private String tenantId;
    private String createdByEmail;
    private SupportCaseCategory category;
    private SupportCasePriority priority;
    private SupportCaseStatus status;
    private String subject;
    private String description;
    private String correlationId;
    private String programmeUid;
    private String pageUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private String slaResponseHint;
    private String companyName;
    private String subscriptionTier;
    private Instant statusUpdatedAt;
    private String statusUpdatedBy;
    private SupportActorType statusUpdatedByType;
    private List<SupportCaseStatus> allowedNextStatuses = new ArrayList<>();

    public String getCaseUid() {
        return caseUid;
    }

    public void setCaseUid(String caseUid) {
        this.caseUid = caseUid;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getSlaResponseHint() {
        return slaResponseHint;
    }

    public void setSlaResponseHint(String slaResponseHint) {
        this.slaResponseHint = slaResponseHint;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
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

    public List<SupportCaseStatus> getAllowedNextStatuses() {
        return allowedNextStatuses;
    }

    public void setAllowedNextStatuses(List<SupportCaseStatus> allowedNextStatuses) {
        this.allowedNextStatuses = allowedNextStatuses;
    }
}
