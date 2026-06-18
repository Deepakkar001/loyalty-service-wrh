package com.loyaltyos.merchants.dto;

import java.math.BigDecimal;

public class UpdateMerchantConfigRequest {

    private BigDecimal earnRateMultiplier;
    private String commissionConfigJson;
    private String settlementCycle;
    private String eligibleCategoriesJson;
    private String capabilitiesJson;

    public BigDecimal getEarnRateMultiplier() { return earnRateMultiplier; }
    public void setEarnRateMultiplier(BigDecimal earnRateMultiplier) { this.earnRateMultiplier = earnRateMultiplier; }

    public String getCommissionConfigJson() { return commissionConfigJson; }
    public void setCommissionConfigJson(String commissionConfigJson) { this.commissionConfigJson = commissionConfigJson; }

    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }

    public String getEligibleCategoriesJson() { return eligibleCategoriesJson; }
    public void setEligibleCategoriesJson(String eligibleCategoriesJson) {
        this.eligibleCategoriesJson = eligibleCategoriesJson;
    }

    public String getCapabilitiesJson() { return capabilitiesJson; }
    public void setCapabilitiesJson(String capabilitiesJson) { this.capabilitiesJson = capabilitiesJson; }
}
