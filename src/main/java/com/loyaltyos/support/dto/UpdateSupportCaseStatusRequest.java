package com.loyaltyos.support.dto;

import com.loyaltyos.support.enums.SupportCaseStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateSupportCaseStatusRequest {

    @NotNull
    private SupportCaseStatus status;

    public SupportCaseStatus getStatus() {
        return status;
    }

    public void setStatus(SupportCaseStatus status) {
        this.status = status;
    }
}
