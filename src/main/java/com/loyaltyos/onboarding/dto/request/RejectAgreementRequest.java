package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectAgreementRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, message = "Rejection reason must be at least 10 characters")
    private String rejectionReason;

    public RejectAgreementRequest() {}

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
