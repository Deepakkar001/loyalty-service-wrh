package com.loyaltyos.onboarding.dto.response;

import java.time.Instant;

public class AdminBusinessCategoryItem {
    private String code;
    private String label;
    /** The original tenant-typed text (only set for tenant submissions). */
    private String submittedLabel;
    private Integer sortOrder;
    private Boolean active;
    /** PENDING_REVIEW / APPROVED / REJECTED. */
    private String status;
    private String submittedByTenantId;
    private String submittedByCompanyName;
    private String submittedByEmail;
    private String decisionReason;
    private String decidedByAdminId;
    private Instant decidedAt;

    public AdminBusinessCategoryItem() {}

    public AdminBusinessCategoryItem(
        String code,
        String label,
        String submittedLabel,
        Integer sortOrder,
        Boolean active,
        String status,
        String submittedByTenantId,
        String submittedByCompanyName,
        String submittedByEmail,
        String decisionReason,
        String decidedByAdminId,
        Instant decidedAt
    ) {
        this.code = code;
        this.label = label;
        this.submittedLabel = submittedLabel;
        this.sortOrder = sortOrder;
        this.active = active;
        this.status = status;
        this.submittedByTenantId = submittedByTenantId;
        this.submittedByCompanyName = submittedByCompanyName;
        this.submittedByEmail = submittedByEmail;
        this.decisionReason = decisionReason;
        this.decidedByAdminId = decidedByAdminId;
        this.decidedAt = decidedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String code;
        private String label;
        private String submittedLabel;
        private Integer sortOrder;
        private Boolean active;
        private String status;
        private String submittedByTenantId;
        private String submittedByCompanyName;
        private String submittedByEmail;
        private String decisionReason;
        private String decidedByAdminId;
        private Instant decidedAt;

        private Builder() {}

        public Builder code(String code) { this.code = code; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder submittedLabel(String submittedLabel) { this.submittedLabel = submittedLabel; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder submittedByTenantId(String submittedByTenantId) { this.submittedByTenantId = submittedByTenantId; return this; }
        public Builder submittedByCompanyName(String submittedByCompanyName) { this.submittedByCompanyName = submittedByCompanyName; return this; }
        public Builder submittedByEmail(String submittedByEmail) { this.submittedByEmail = submittedByEmail; return this; }
        public Builder decisionReason(String decisionReason) { this.decisionReason = decisionReason; return this; }
        public Builder decidedByAdminId(String decidedByAdminId) { this.decidedByAdminId = decidedByAdminId; return this; }
        public Builder decidedAt(Instant decidedAt) { this.decidedAt = decidedAt; return this; }

        public AdminBusinessCategoryItem build() {
            return new AdminBusinessCategoryItem(
                code,
                label,
                submittedLabel,
                sortOrder,
                active,
                status,
                submittedByTenantId,
                submittedByCompanyName,
                submittedByEmail,
                decisionReason,
                decidedByAdminId,
                decidedAt
            );
        }
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getSubmittedLabel() { return submittedLabel; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getActive() { return active; }
    public String getStatus() { return status; }
    public String getSubmittedByTenantId() { return submittedByTenantId; }
    public String getSubmittedByCompanyName() { return submittedByCompanyName; }
    public String getSubmittedByEmail() { return submittedByEmail; }
    public String getDecisionReason() { return decisionReason; }
    public String getDecidedByAdminId() { return decidedByAdminId; }
    public Instant getDecidedAt() { return decidedAt; }
}
