package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectBusinessCategoryRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 5, max = 500, message = "Rejection reason must be 5-500 characters")
    private String reason;

    public RejectBusinessCategoryRequest() {}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
