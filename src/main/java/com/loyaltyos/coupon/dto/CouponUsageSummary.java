package com.loyaltyos.coupon.dto;

import java.math.BigDecimal;

public class CouponUsageSummary {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private int totalCoupons;
    private int activeCoupons;
    private long redemptionsInPeriod;
    private long redemptionsPriorPeriod;
    private long uniqueCustomersInPeriod;
    private BigDecimal totalDiscountInPeriod;
    private BigDecimal totalOrderValueInPeriod;
    private BigDecimal totalPointsCreditedInPeriod;
    private BigDecimal periodOverPeriodChangePct;
    private String currency;

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public int getTotalCoupons() { return totalCoupons; }
    public void setTotalCoupons(int totalCoupons) { this.totalCoupons = totalCoupons; }
    public int getActiveCoupons() { return activeCoupons; }
    public void setActiveCoupons(int activeCoupons) { this.activeCoupons = activeCoupons; }
    public long getRedemptionsInPeriod() { return redemptionsInPeriod; }
    public void setRedemptionsInPeriod(long redemptionsInPeriod) { this.redemptionsInPeriod = redemptionsInPeriod; }
    public long getRedemptionsPriorPeriod() { return redemptionsPriorPeriod; }
    public void setRedemptionsPriorPeriod(long redemptionsPriorPeriod) { this.redemptionsPriorPeriod = redemptionsPriorPeriod; }
    public long getUniqueCustomersInPeriod() { return uniqueCustomersInPeriod; }
    public void setUniqueCustomersInPeriod(long uniqueCustomersInPeriod) { this.uniqueCustomersInPeriod = uniqueCustomersInPeriod; }
    public BigDecimal getTotalDiscountInPeriod() { return totalDiscountInPeriod; }
    public void setTotalDiscountInPeriod(BigDecimal totalDiscountInPeriod) { this.totalDiscountInPeriod = totalDiscountInPeriod; }
    public BigDecimal getTotalOrderValueInPeriod() { return totalOrderValueInPeriod; }
    public void setTotalOrderValueInPeriod(BigDecimal totalOrderValueInPeriod) { this.totalOrderValueInPeriod = totalOrderValueInPeriod; }
    public BigDecimal getTotalPointsCreditedInPeriod() { return totalPointsCreditedInPeriod; }
    public void setTotalPointsCreditedInPeriod(BigDecimal totalPointsCreditedInPeriod) { this.totalPointsCreditedInPeriod = totalPointsCreditedInPeriod; }
    public BigDecimal getPeriodOverPeriodChangePct() { return periodOverPeriodChangePct; }
    public void setPeriodOverPeriodChangePct(BigDecimal periodOverPeriodChangePct) { this.periodOverPeriodChangePct = periodOverPeriodChangePct; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
