package com.loyaltyos.referrals.dto;

import java.util.ArrayList;
import java.util.List;

public class ReferralStageRewards {

    private List<ReferralIssuanceLine> issuanceLines = new ArrayList<>();
    private List<ReferralVoucherGrantLine> voucherGrants = new ArrayList<>();

    public List<ReferralIssuanceLine> getIssuanceLines() {
        return issuanceLines;
    }

    public void setIssuanceLines(List<ReferralIssuanceLine> issuanceLines) {
        this.issuanceLines = issuanceLines != null ? issuanceLines : new ArrayList<>();
    }

    public List<ReferralVoucherGrantLine> getVoucherGrants() {
        return voucherGrants;
    }

    public void setVoucherGrants(List<ReferralVoucherGrantLine> voucherGrants) {
        this.voucherGrants = voucherGrants != null ? voucherGrants : new ArrayList<>();
    }
}
