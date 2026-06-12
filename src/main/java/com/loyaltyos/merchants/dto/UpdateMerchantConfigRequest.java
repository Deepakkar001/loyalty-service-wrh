package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;

public class UpdateMerchantConfigRequest {

    private BigDecimal earnRateMultiplier;
    private String commissionConfigJson;
    private String settlementCycle;

    public BigDecimal getEarnRateMultiplier() { return earnRateMultiplier; }
    public void setEarnRateMultiplier(BigDecimal earnRateMultiplier) { this.earnRateMultiplier = earnRateMultiplier; }

    public String getCommissionConfigJson() { return commissionConfigJson; }
    public void setCommissionConfigJson(String commissionConfigJson) { this.commissionConfigJson = commissionConfigJson; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }
}
