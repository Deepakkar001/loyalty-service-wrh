package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.BusinessCategoryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ref_business_category")
public class RefBusinessCategory {

    @Id
    @Column(name = "code", length = 32, nullable = false, updatable = false)
    private String code;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    /**
     * Moderation lifecycle. Existing rows default to {@link BusinessCategoryStatus#APPROVED}
     * via the migration so behaviour for seeded data is unchanged.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private BusinessCategoryStatus status = BusinessCategoryStatus.APPROVED;

    @Column(name = "submitted_by_tenant_id", length = 36)
    private String submittedByTenantId;

    @Column(name = "submitted_label", length = 150)
    private String submittedLabel;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "decided_by_admin_id", length = 36)
    private String decidedByAdminId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** JPA requires a no-arg constructor. */
    public RefBusinessCategory() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BusinessCategoryStatus getStatus() {
        return status;
    }

    public void setStatus(BusinessCategoryStatus status) {
        this.status = status;
    }

    public String getSubmittedByTenantId() {
        return submittedByTenantId;
    }

    public void setSubmittedByTenantId(String submittedByTenantId) {
        this.submittedByTenantId = submittedByTenantId;
    }

    public String getSubmittedLabel() {
        return submittedLabel;
    }

    public void setSubmittedLabel(String submittedLabel) {
        this.submittedLabel = submittedLabel;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getDecidedByAdminId() {
        return decidedByAdminId;
    }

    public void setDecidedByAdminId(String decidedByAdminId) {
        this.decidedByAdminId = decidedByAdminId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
