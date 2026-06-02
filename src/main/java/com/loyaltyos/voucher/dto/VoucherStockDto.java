package com.loyaltyos.voucher.dto;

public class VoucherStockDto {

    private String catalogRewardUid;
    private long available;
    private long lowStockThreshold;
    private boolean lowStock;

    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public long getAvailable() { return available; }
    public void setAvailable(long available) { this.available = available; }
    public long getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(long lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public boolean isLowStock() { return lowStock; }
    public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }
}
