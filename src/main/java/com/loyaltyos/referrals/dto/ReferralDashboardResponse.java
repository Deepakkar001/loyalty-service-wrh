package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public class ReferralDashboardResponse {

    private long totalReferrals;
    private long signedUp;
    private long rewarded;
    private long fraudFlagged;
    private BigDecimal totalPointsIssued = BigDecimal.ZERO;
    /** Percentage of referrals that reached REWARDED (0–100). */
    private double conversionRatePercent;
    private BigDecimal averagePointsPerReferral = BigDecimal.ZERO;

    public long getTotalReferrals() {
        return totalReferrals;
    }

    public void setTotalReferrals(long totalReferrals) {
        this.totalReferrals = totalReferrals;
    }

    public long getSignedUp() {
        return signedUp;
    }

    public void setSignedUp(long signedUp) {
        this.signedUp = signedUp;
    }

    public long getRewarded() {
        return rewarded;
    }

    public void setRewarded(long rewarded) {
        this.rewarded = rewarded;
    }

    public long getFraudFlagged() {
        return fraudFlagged;
    }

    public void setFraudFlagged(long fraudFlagged) {
        this.fraudFlagged = fraudFlagged;
    }

    public BigDecimal getTotalPointsIssued() {
        return totalPointsIssued;
    }

    public void setTotalPointsIssued(BigDecimal totalPointsIssued) {
        this.totalPointsIssued = totalPointsIssued;
    }

    public double getConversionRatePercent() {
        return conversionRatePercent;
    }

    public void setConversionRatePercent(double conversionRatePercent) {
        this.conversionRatePercent = conversionRatePercent;
    }

    public BigDecimal getAveragePointsPerReferral() {
        return averagePointsPerReferral;
    }

    public void setAveragePointsPerReferral(BigDecimal averagePointsPerReferral) {
        this.averagePointsPerReferral = averagePointsPerReferral;
    }
}
