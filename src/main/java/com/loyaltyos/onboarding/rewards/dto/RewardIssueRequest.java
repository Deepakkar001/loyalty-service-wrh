package com.loyaltyos.onboarding.rewards.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue request: tenant comes from JWT (not this body). Programme and {@code eventId} scope the issuance.
 * <p><b>Idempotency:</b> uniqueness is enforced per {@code (tenantId, customerId, idempotencyKey)} on each
 * {@link RewardIssueCommandDto#getIdempotencyKey() reward command}, not on {@code eventId} alone. Keys must be
 * stable across retries and unique across distinct business events for the same customer.
 */
public class RewardIssueRequest {

    @NotBlank
    @Size(max = 64)
    private String programmeUid = "default";

    @NotBlank
    @Size(max = 128)
    private String customerId;

    @NotBlank
    @Size(max = 128)
    private String eventId;

    @NotNull
    @Valid
    private List<RewardIssueCommandDto> rewardCommands = new ArrayList<>();

    /** Optional narrative for ledger description. */
    @Size(max = 256)
    private String narrative;

    public RewardIssueRequest() {}

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<RewardIssueCommandDto> getRewardCommands() {
        return rewardCommands;
    }

    public void setRewardCommands(List<RewardIssueCommandDto> rewardCommands) {
        this.rewardCommands = rewardCommands != null ? rewardCommands : new ArrayList<>();
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }
}
