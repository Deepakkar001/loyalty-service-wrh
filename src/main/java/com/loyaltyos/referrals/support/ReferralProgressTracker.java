package com.loyaltyos.referrals.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.entity.Referral;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReferralProgressTracker {

    private ReferralProgressTracker() {}

    public static void recordPurchase(
        Referral referral,
        String eventId,
        BigDecimal amount,
        Instant at,
        Map<String, Object> eventMetadata,
        ObjectMapper objectMapper
    ) {
        ProgressState state = load(referral, objectMapper);
        PurchaseEvent pe = new PurchaseEvent();
        pe.setEventId(eventId);
        pe.setAmount(amount != null ? amount : BigDecimal.ZERO);
        pe.setAt(at != null ? at : Instant.now());
        if (eventMetadata != null) {
            if (eventMetadata.get("merchantId") != null) {
                pe.setMerchantId(String.valueOf(eventMetadata.get("merchantId")));
            }
            if (eventMetadata.get("merchant_id") != null) {
                pe.setMerchantId(String.valueOf(eventMetadata.get("merchant_id")));
            }
            if (eventMetadata.get("category") != null) {
                pe.setCategory(String.valueOf(eventMetadata.get("category")));
            }
            if (eventMetadata.get("Channel") != null) {
                pe.setChannel(String.valueOf(eventMetadata.get("Channel")));
            }
            if (eventMetadata.get("channel") != null) {
                pe.setChannel(String.valueOf(eventMetadata.get("channel")));
            }
            if (eventMetadata.get("sku") != null) {
                pe.setSku(String.valueOf(eventMetadata.get("sku")));
            }
            if (eventMetadata.get("SKU") != null) {
                pe.setSku(String.valueOf(eventMetadata.get("SKU")));
            }
            if (eventMetadata.get("region") != null) {
                pe.setRegion(String.valueOf(eventMetadata.get("region")));
            }
            if (eventMetadata.get("Region") != null) {
                pe.setRegion(String.valueOf(eventMetadata.get("Region")));
            }
            if (eventMetadata.get("geo") != null) {
                pe.setRegion(String.valueOf(eventMetadata.get("geo")));
            }
            if (eventMetadata.get("country") != null) {
                pe.setRegion(String.valueOf(eventMetadata.get("country")));
            }
            pe.setMetadata(ReferralMetadataSupport.snapshot(eventMetadata));
        }
        state.getPurchases().add(pe);
        save(referral, state, objectMapper);
    }

    public static List<PurchaseEvent> purchasesInWindow(List<PurchaseEvent> all, int windowDays, Instant now) {
        if (windowDays <= 0) {
            return all;
        }
        Instant cutoff = now.minusSeconds(windowDays * 86400L);
        return all.stream()
            .filter(p -> p.getAt() != null && !p.getAt().isBefore(cutoff))
            .toList();
    }

    public static BigDecimal spendInWindow(List<PurchaseEvent> inWindow) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseEvent p : inWindow) {
            if (p.getAmount() != null) {
                total = total.add(p.getAmount());
            }
        }
        return total;
    }

    public static ProgressState load(Referral referral, ObjectMapper objectMapper) {
        if (referral.getProgressJson() == null || referral.getProgressJson().isBlank()) {
            return new ProgressState();
        }
        try {
            return objectMapper.readValue(referral.getProgressJson(), ProgressState.class);
        } catch (Exception e) {
            return new ProgressState();
        }
    }

    public static void save(Referral referral, ProgressState state, ObjectMapper objectMapper) {
        try {
            referral.setProgressJson(objectMapper.writeValueAsString(state));
        } catch (Exception e) {
            referral.setProgressJson("{\"purchases\":[]}");
        }
    }

    public static class ProgressState {
        private List<PurchaseEvent> purchases = new ArrayList<>();

        public List<PurchaseEvent> getPurchases() {
            return purchases;
        }

        public void setPurchases(List<PurchaseEvent> purchases) {
            this.purchases = purchases != null ? purchases : new ArrayList<>();
        }
    }

    public static class PurchaseEvent {
        private String eventId;
        private BigDecimal amount = BigDecimal.ZERO;
        private Instant at;
        private String merchantId;
        private String category;
        private String channel;
        private String sku;
        private String region;
        private Map<String, String> metadata = new LinkedHashMap<>();

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Instant getAt() { return at; }
        public void setAt(Instant at) { this.at = at; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata != null ? metadata : new LinkedHashMap<>();
        }
    }
}
