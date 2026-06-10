package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public class ReferralEffectivenessTrendRow {

    private String periodStart;
    private long referrals;
    private long signedUp;
    private long rewarded;
    private BigDecimal conversionRatePercent;
    private BigDecimal refereeSpend;

    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }
    public long getReferrals() { return referrals; }
    public void setReferrals(long referrals) { this.referrals = referrals; }
    public long getSignedUp() { return signedUp; }
    public void setSignedUp(long signedUp) { this.signedUp = signedUp; }
    public long getRewarded() { return rewarded; }
    public void setRewarded(long rewarded) { this.rewarded = rewarded; }
    public BigDecimal getConversionRatePercent() { return conversionRatePercent; }
    public void setConversionRatePercent(BigDecimal conversionRatePercent) { this.conversionRatePercent = conversionRatePercent; }
    public BigDecimal getRefereeSpend() { return refereeSpend; }
    public void setRefereeSpend(BigDecimal refereeSpend) { this.refereeSpend = refereeSpend; }
}
