package com.loyaltyos.referrals.support;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReferralMetadataSupport {

    private static final Set<String> RESERVED_KEYS = Set.of(
        "transactionid",
        "timestamp",
        "eventtype",
        "customerid",
        "eventid",
        "id",
        "amount"
    );

    private ReferralMetadataSupport() {}

    public static Map<String, String> snapshot(Map<String, Object> eventMetadata) {
        Map<String, String> out = new LinkedHashMap<>();
        if (eventMetadata == null) {
            return out;
        }
        for (Map.Entry<String, Object> e : eventMetadata.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            String key = e.getKey().trim();
            if (key.isEmpty() || RESERVED_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            out.put(key, String.valueOf(e.getValue()).trim());
        }
        return out;
    }
}
