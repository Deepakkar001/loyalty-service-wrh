package com.loyaltyos.referrals.model;

import java.math.BigDecimal;

public class ReferralPointsBudget {

    public enum Period {
        LIFETIME,
        CALENDAR_MONTH,
        ROLLING_DAY
    }

    private BigDecimal maxPoints;
    private Period period = Period.LIFETIME;
    private Integer windowDays;

    public BigDecimal getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(BigDecimal maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Period getPeriod() {
        return period != null ? period : Period.LIFETIME;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Integer getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(Integer windowDays) {
        this.windowDays = windowDays;
    }
}
