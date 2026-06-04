package com.loyaltyos.referrals.dto;

public class ReferralTimeToPurchaseResponse {

    private double averageHoursToFirstPurchase;
    private long sampleSize;

    public double getAverageHoursToFirstPurchase() {
        return averageHoursToFirstPurchase;
    }

    public void setAverageHoursToFirstPurchase(double averageHoursToFirstPurchase) {
        this.averageHoursToFirstPurchase = averageHoursToFirstPurchase;
    }

    public long getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(long sampleSize) {
        this.sampleSize = sampleSize;
    }
}
