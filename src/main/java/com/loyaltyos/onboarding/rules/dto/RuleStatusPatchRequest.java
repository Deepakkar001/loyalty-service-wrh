package com.loyaltyos.onboarding.rules.dto;

import com.loyaltyos.onboarding.rules.enums.RuleStatus;
import jakarta.validation.constraints.NotNull;

public class RuleStatusPatchRequest {
    @NotNull
    private RuleStatus status;

    public RuleStatusPatchRequest() {}

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }
}
