package com.loyaltyos.onboarding.rewards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RewardReverseRequest {

    @NotBlank
    @Size(max = 128)
    private String customerId;

    @NotBlank
    @Size(max = 64)
    private String programmeUid = "default";

    @NotNull
    private Long creditLedgerId;

    @NotBlank
    @Size(max = 128)
    private String reversalIdempotencyKey;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public Long getCreditLedgerId() {
        return creditLedgerId;
    }

    public void setCreditLedgerId(Long creditLedgerId) {
        this.creditLedgerId = creditLedgerId;
    }

    public String getReversalIdempotencyKey() {
        return reversalIdempotencyKey;
    }

    public void setReversalIdempotencyKey(String reversalIdempotencyKey) {
        this.reversalIdempotencyKey = reversalIdempotencyKey;
    }
}
