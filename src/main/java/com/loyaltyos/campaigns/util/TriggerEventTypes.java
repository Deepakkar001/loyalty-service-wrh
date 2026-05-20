package com.loyaltyos.campaigns.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Comma-separated trigger event types on campaigns (e.g. {@code PURCHASE,REFUND}). */
public final class TriggerEventTypes {

    public static final int MAX_TYPES = 16;
    public static final int MAX_TYPE_LENGTH = 64;
    public static final int MAX_SERIALIZED_LENGTH = 512;

    private TriggerEventTypes() {}

    public static List<String> parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : stored.split("[,;]")) {
            String token = part.trim().toUpperCase(Locale.ROOT);
            if (!token.isEmpty()) {
                seen.add(token);
            }
        }
        return List.copyOf(seen);
    }

    public static String normalize(String raw) {
        return String.join(",", parse(raw));
    }

    public static boolean contains(String stored, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String needle = candidate.trim().toUpperCase(Locale.ROOT);
        for (String token : parse(stored)) {
            if (token.equals(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Campaign earn rules store one {@code trigger_event_type} per row; campaigns may list several
     * comma-separated types. Resolves and validates a single type from user input.
     */
    public static String resolveSingleForCampaignRule(String campaignStored, String candidateRaw) {
        List<String> allowed = parse(campaignStored);
        List<String> candidateParts = parse(candidateRaw);
        if (allowed.isEmpty()) {
            if (candidateParts.isEmpty()) {
                throw new IllegalArgumentException("triggerEventType is required");
            }
            if (candidateParts.size() > 1) {
                throw new IllegalArgumentException("Campaign rules must use a single event type");
            }
            return candidateParts.get(0);
        }
        if (candidateParts.isEmpty()) {
            throw new IllegalArgumentException("triggerEventType is required");
        }
        if (candidateParts.size() > 1) {
            throw new IllegalArgumentException(
                "Campaign rules must use a single event type. Choose one of: " + String.join(", ", allowed)
            );
        }
        String single = candidateParts.get(0);
        if (!allowed.contains(single)) {
            throw new IllegalArgumentException(
                "triggerEventType must be one of the campaign event types: " + String.join(", ", allowed)
            );
        }
        return single;
    }

    public static void validateSerialized(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("triggerEventType is required");
        }
        List<String> types = parse(raw);
        if (types.isEmpty()) {
            throw new IllegalArgumentException("triggerEventType is required");
        }
        if (types.size() > MAX_TYPES) {
            throw new IllegalArgumentException("triggerEventType supports at most " + MAX_TYPES + " event types");
        }
        for (String type : types) {
            if (type.length() > MAX_TYPE_LENGTH) {
                throw new IllegalArgumentException("Each event type must be at most " + MAX_TYPE_LENGTH + " characters");
            }
        }
        String serialized = String.join(",", types);
        if (serialized.length() > MAX_SERIALIZED_LENGTH) {
            throw new IllegalArgumentException("triggerEventType exceeds " + MAX_SERIALIZED_LENGTH + " characters");
        }
    }
}
