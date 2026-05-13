package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/v1/admin/business-categories/{code}/deactivate}.
 * Reason is optional (free-text audit trail) but recommended.
 */
public class DeactivateBusinessCategoryRequest {

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;

    public DeactivateBusinessCategoryRequest() {}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
