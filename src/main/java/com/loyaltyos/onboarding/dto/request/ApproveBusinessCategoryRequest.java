package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.Size;
public class ApproveBusinessCategoryRequest {

    /** Optional admin-edited label that replaces the tenant-typed text. */
    @Size(max = 100, message = "Label must be at most 100 characters")
    private String label;

    /** Optional sort order override. */
    private Integer sortOrder;

    public ApproveBusinessCategoryRequest() {}

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
}
