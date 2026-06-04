package com.loyaltyos.referrals.dto;

import com.loyaltyos.referrals.enums.ReferralProgrammeStatus;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import java.time.Instant;

public class ReferralProgrammeUpsertRequest {

    private String programmeUid = "default";
    private String name;
    private String description;
    private ReferralProgrammeStatus status;
    private Instant validFrom;
    private Instant validUntil;
    private ReferralProgrammeConfig config;
    private Integer maxReferralsPerCustomer;

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReferralProgrammeStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralProgrammeStatus status) {
        this.status = status;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public ReferralProgrammeConfig getConfig() {
        return config;
    }

    public void setConfig(ReferralProgrammeConfig config) {
        this.config = config;
    }

    public Integer getMaxReferralsPerCustomer() {
        return maxReferralsPerCustomer;
    }

    public void setMaxReferralsPerCustomer(Integer maxReferralsPerCustomer) {
        this.maxReferralsPerCustomer = maxReferralsPerCustomer;
    }
}
