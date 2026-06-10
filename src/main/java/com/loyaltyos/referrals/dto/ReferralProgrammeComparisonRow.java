package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public class ReferralProgrammeComparisonRow {

    private String programmeUid;
    private String programmeName;
    private long totalReferrals;
    private long rewarded;
    private BigDecimal conversionRatePercent;
    private BigDecimal totalRefereeSpend;
    private BigDecimal totalRewardPoints;

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
    public long getTotalReferrals() { return totalReferrals; }
    public void setTotalReferrals(long totalReferrals) { this.totalReferrals = totalReferrals; }
    public long getRewarded() { return rewarded; }
    public void setRewarded(long rewarded) { this.rewarded = rewarded; }
    public BigDecimal getConversionRatePercent() { return conversionRatePercent; }
    public void setConversionRatePercent(BigDecimal conversionRatePercent) { this.conversionRatePercent = conversionRatePercent; }
    public BigDecimal getTotalRefereeSpend() { return totalRefereeSpend; }
    public void setTotalRefereeSpend(BigDecimal totalRefereeSpend) { this.totalRefereeSpend = totalRefereeSpend; }
    public BigDecimal getTotalRewardPoints() { return totalRewardPoints; }
    public void setTotalRewardPoints(BigDecimal totalRewardPoints) { this.totalRewardPoints = totalRewardPoints; }
}
