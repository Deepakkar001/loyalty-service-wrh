package com.loyaltyos.voucher.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class VoucherStockBreakdownDto {

    private String catalogRewardUid;
    private long totalAvailable;
    private Map<String, Long> stockByFaceValue = new LinkedHashMap<>();

    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public long getTotalAvailable() { return totalAvailable; }
    public void setTotalAvailable(long totalAvailable) { this.totalAvailable = totalAvailable; }
    public Map<String, Long> getStockByFaceValue() { return stockByFaceValue; }
    public void setStockByFaceValue(Map<String, Long> stockByFaceValue) { this.stockByFaceValue = stockByFaceValue; }

    public static String faceValueKey(BigDecimal faceValue) {
        return faceValue == null ? "0" : faceValue.stripTrailingZeros().toPlainString();
    }
}
