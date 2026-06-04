package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import com.loyaltyos.referrals.support.ReferralProgressTracker.PurchaseEvent;
import java.util.Locale;
import java.util.Map;

final class ReferralCriteriaMatcher {

    private ReferralCriteriaMatcher() {}

    static boolean matchesPurchaseEvent(PurchaseEvent purchase, ReferralRuleCriteria criteria) {
        if (criteria == null) {
            return true;
        }
        if (!matchesStringFilter(criteria.getMerchantId(), purchase.getMerchantId())) {
            return false;
        }
        if (!matchesStringFilter(criteria.getCategory(), purchase.getCategory())) {
            return false;
        }
        if (!matchesStringFilter(criteria.getChannel(), purchase.getChannel())) {
            return false;
        }
        if (!matchesStringFilter(criteria.getSku(), purchase.getSku())) {
            return false;
        }
        if (!matchesStringFilter(criteria.getRegion(), purchase.getRegion())) {
            return false;
        }
        return matchesMetadataFilters(criteria, purchase.getMetadata());
    }

    static boolean matchesEventMetadata(ReferralRuleCriteria criteria, Map<String, Object> eventMetadata) {
        if (criteria == null) {
            return true;
        }
        Map<String, String> snapshot = ReferralMetadataSupport.snapshot(eventMetadata);
        if (!matchesStringFilter(criteria.getMerchantId(), firstMeta(snapshot, eventMetadata, "merchantId", "merchant_id"))) {
            return false;
        }
        if (!matchesStringFilter(criteria.getCategory(), firstMeta(snapshot, eventMetadata, "category"))) {
            return false;
        }
        if (!matchesStringFilter(criteria.getChannel(), firstMeta(snapshot, eventMetadata, "channel", "Channel"))) {
            return false;
        }
        if (!matchesStringFilter(criteria.getSku(), firstMeta(snapshot, eventMetadata, "sku", "SKU"))) {
            return false;
        }
        if (!matchesStringFilter(criteria.getRegion(), firstMeta(snapshot, eventMetadata, "region", "Region", "geo", "country"))) {
            return false;
        }
        return matchesMetadataFilters(criteria, snapshot);
    }

    static boolean hasAnyMetadataFilter(ReferralRuleCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        return isSet(criteria.getMerchantId())
            || isSet(criteria.getCategory())
            || isSet(criteria.getChannel())
            || isSet(criteria.getSku())
            || isSet(criteria.getRegion())
            || (criteria.getMetadataFilters() != null && !criteria.getMetadataFilters().isEmpty());
    }

    private static boolean matchesMetadataFilters(ReferralRuleCriteria criteria, Map<String, String> actual) {
        if (criteria.getMetadataFilters() == null || criteria.getMetadataFilters().isEmpty()) {
            return true;
        }
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> expected : criteria.getMetadataFilters().entrySet()) {
            if (expected.getKey() == null || expected.getValue() == null || expected.getValue().isBlank()) {
                continue;
            }
            String actualVal = lookupKey(actual, expected.getKey());
            if (!matchesStringFilter(expected.getValue(), actualVal)) {
                return false;
            }
        }
        return true;
    }

    private static String firstMeta(
        Map<String, String> snapshot,
        Map<String, Object> raw,
        String... keys
    ) {
        for (String key : keys) {
            String fromSnapshot = lookupKey(snapshot, key);
            if (fromSnapshot != null && !fromSnapshot.isBlank()) {
                return fromSnapshot;
            }
            if (raw != null && raw.containsKey(key) && raw.get(key) != null) {
                return String.valueOf(raw.get(key));
            }
        }
        return null;
    }

    private static String lookupKey(Map<String, String> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        if (map.containsKey(key)) {
            return map.get(key);
        }
        String lower = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static boolean matchesStringFilter(String expected, String actual) {
        if (!isSet(expected)) {
            return true;
        }
        if (actual == null || actual.isBlank()) {
            return false;
        }
        return expected.trim().equalsIgnoreCase(actual.trim());
    }

    private static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }
}
