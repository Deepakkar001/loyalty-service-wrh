package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public class ReferralFunnelStageRow {

    private String stage;
    private long count;
    private BigDecimal sharePercent;

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public BigDecimal getSharePercent() { return sharePercent; }
    public void setSharePercent(BigDecimal sharePercent) { this.sharePercent = sharePercent; }
}
