package com.loyaltyos.onboarding.dto;

import com.loyaltyos.onboarding.entity.Programme.ProgrammeStatus;
import jakarta.validation.constraints.NotNull;

public class ProgrammeStatusPatchRequest {

    @NotNull
    private ProgrammeStatus status;

    public ProgrammeStatusPatchRequest() {}

    public ProgrammeStatus getStatus() {
        return status;
    }

    public void setStatus(ProgrammeStatus status) {
        this.status = status;
    }
}
