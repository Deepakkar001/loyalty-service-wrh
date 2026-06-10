package com.loyaltyos.coupon.dto;

import java.util.ArrayList;
import java.util.List;

public class CouponUsageReportResponse {

    private CouponUsageSummary summary = new CouponUsageSummary();
    private List<CouponUsageTrendRow> dailyRedemptions = new ArrayList<>();
    private List<CouponChannelBreakdownRow> byChannel = new ArrayList<>();
    private List<CouponTypeBreakdownRow> byCouponType = new ArrayList<>();
    private List<CouponPerformanceRow> coupons = new ArrayList<>();

    public CouponUsageSummary getSummary() { return summary; }
    public void setSummary(CouponUsageSummary summary) { this.summary = summary; }
    public List<CouponUsageTrendRow> getDailyRedemptions() { return dailyRedemptions; }
    public void setDailyRedemptions(List<CouponUsageTrendRow> dailyRedemptions) { this.dailyRedemptions = dailyRedemptions; }
    public List<CouponChannelBreakdownRow> getByChannel() { return byChannel; }
    public void setByChannel(List<CouponChannelBreakdownRow> byChannel) { this.byChannel = byChannel; }
    public List<CouponTypeBreakdownRow> getByCouponType() { return byCouponType; }
    public void setByCouponType(List<CouponTypeBreakdownRow> byCouponType) { this.byCouponType = byCouponType; }
    public List<CouponPerformanceRow> getCoupons() { return coupons; }
    public void setCoupons(List<CouponPerformanceRow> coupons) { this.coupons = coupons; }
}
