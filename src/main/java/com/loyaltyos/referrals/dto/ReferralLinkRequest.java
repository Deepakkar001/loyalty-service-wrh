package com.loyaltyos.referrals.dto;

import com.loyaltyos.referrals.model.ReferralLinkSignals;
import jakarta.validation.constraints.NotBlank;

public class ReferralLinkRequest {

    private String programmeUid = "default";

    @NotBlank
    private String referralCode;

    @NotBlank
    private String refereeCustomerId;

    /** Optional — used for fraud matching when configured in programme fraudPolicy. */
    private ReferralLinkSignals referrerSignals;

    private ReferralLinkSignals refereeSignals;

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public String getRefereeCustomerId() {
        return refereeCustomerId;
    }

    public void setRefereeCustomerId(String refereeCustomerId) {
        this.refereeCustomerId = refereeCustomerId;
    }

    public ReferralLinkSignals getReferrerSignals() {
        return referrerSignals;
    }

    public void setReferrerSignals(ReferralLinkSignals referrerSignals) {
        this.referrerSignals = referrerSignals;
    }

    public ReferralLinkSignals getRefereeSignals() {
        return refereeSignals;
    }

    public void setRefereeSignals(ReferralLinkSignals refereeSignals) {
        this.refereeSignals = refereeSignals;
    }
}
