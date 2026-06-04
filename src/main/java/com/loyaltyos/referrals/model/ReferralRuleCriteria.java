package com.loyaltyos.referrals.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenant-defined criteria for a referral milestone rule. Unset fields are not enforced.
 */
public class ReferralRuleCriteria {

    /** Only purchases within the last N days count (optional). */
    private Integer windowDays;
    /** Minimum number of qualifying purchases in scope (default 1 when enforced). */
    private Integer minPurchaseCount;
    /** Minimum cumulative spend in scope. */
    private BigDecimal minSpend;
    /** When true, only the first qualifying purchase in scope satisfies the rule. */
    private Boolean firstPurchaseOnly;
    private String merchantId;
    private String category;
    private String channel;
    private String sku;
    /** Region / geo code (country, state, or tenant-defined region id). */
    private String region;
    /**
     * Integration event types that satisfy an {@link ReferralRuleTrigger#INTEGRATION_EVENT} rule
     * (case-insensitive match).
     */
    private List<String> eventTypes = new ArrayList<>();
    /** Equality filters on additional event / purchase metadata (schema-driven fields). */
    private Map<String, String> metadataFilters = new LinkedHashMap<>();

    public Integer getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(Integer windowDays) {
        this.windowDays = windowDays;
    }

    public Integer getMinPurchaseCount() {
        return minPurchaseCount;
    }

    public void setMinPurchaseCount(Integer minPurchaseCount) {
        this.minPurchaseCount = minPurchaseCount;
    }

    public BigDecimal getMinSpend() {
        return minSpend;
    }

    public void setMinSpend(BigDecimal minSpend) {
        this.minSpend = minSpend;
    }

    public Boolean getFirstPurchaseOnly() {
        return firstPurchaseOnly;
    }

    public void setFirstPurchaseOnly(Boolean firstPurchaseOnly) {
        this.firstPurchaseOnly = firstPurchaseOnly;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<String> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<String> eventTypes) {
        this.eventTypes = eventTypes != null ? eventTypes : new ArrayList<>();
    }

    public Map<String, String> getMetadataFilters() {
        return metadataFilters;
    }

    public void setMetadataFilters(Map<String, String> metadataFilters) {
        this.metadataFilters = metadataFilters != null ? metadataFilters : new LinkedHashMap<>();
    }
}
