package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReferralEvaluationResult {

    private List<ReferralIssuanceLine> issuanceLines = new ArrayList<>();
    private List<ReferralVoucherGrantLine> voucherGrants = new ArrayList<>();
    private String referralUid;
    private BigDecimal referralPointsAwarded = BigDecimal.ZERO;

    public static ReferralEvaluationResult empty() {
        return new ReferralEvaluationResult();
    }

    public List<ReferralIssuanceLine> getIssuanceLines() {
        return issuanceLines;
    }

    public void setIssuanceLines(List<ReferralIssuanceLine> issuanceLines) {
        this.issuanceLines = issuanceLines != null ? issuanceLines : new ArrayList<>();
    }

    public BigDecimal getReferralPointsAwarded() {
        return referralPointsAwarded;
    }

    public void setReferralPointsAwarded(BigDecimal referralPointsAwarded) {
        this.referralPointsAwarded = referralPointsAwarded;
    }

    public List<ReferralVoucherGrantLine> getVoucherGrants() {
        return voucherGrants;
    }

    public void setVoucherGrants(List<ReferralVoucherGrantLine> voucherGrants) {
        this.voucherGrants = voucherGrants != null ? voucherGrants : new ArrayList<>();
    }

    public String getReferralUid() {
        return referralUid;
    }

    public void setReferralUid(String referralUid) {
        this.referralUid = referralUid;
    }
}
