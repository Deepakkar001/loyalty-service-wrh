package com.loyaltyos.referrals.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.referrals.dto.ReferralRuleSchemaResponse;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Filterable event payload fields from tenant programme {@code eventSchema} (aligned with portal event-schema setup).
 */
public final class ReferralProgrammeEventSchemaCatalog {

    private static final Set<String> SYSTEM_FIELDS = Set.of(
        "transactionid",
        "timestamp",
        "eventtype",
        "customerid",
        "eventid",
        "id"
    );

    private static final Set<String> PURCHASE_ENGINE_KEYS = Set.of(
        "minpurchasecount",
        "minspend",
        "windowdays",
        "firstpurchaseonly"
    );

    private static final Map<String, String> FIRST_CLASS_TO_SCHEMA = Map.ofEntries(
        Map.entry("merchantid", "merchantid"),
        Map.entry("category", "category"),
        Map.entry("channel", "channel"),
        Map.entry("sku", "sku"),
        Map.entry("region", "region")
    );

    private final Set<String> eventTypes;
    private final Map<String, Set<String>> filterableFieldsByEvent;

    private ReferralProgrammeEventSchemaCatalog(Set<String> eventTypes, Map<String, Set<String>> filterableFieldsByEvent) {
        this.eventTypes = eventTypes;
        this.filterableFieldsByEvent = filterableFieldsByEvent;
    }

    public static ReferralProgrammeEventSchemaCatalog empty() {
        return new ReferralProgrammeEventSchemaCatalog(Set.of(), Map.of());
    }

    public static ReferralProgrammeEventSchemaCatalog fromProgrammeConfigJson(JsonNode programmeRoot) {
        if (programmeRoot == null || programmeRoot.isMissingNode() || programmeRoot.isNull()) {
            return empty();
        }
        JsonNode es = programmeRoot.path("eventSchema");
        if (es.isMissingNode() || es.isNull()) {
            return empty();
        }

        Set<String> types = new LinkedHashSet<>();
        Map<String, Set<String>> fieldsByEvent = new LinkedHashMap<>();

        JsonNode defs = es.path("eventDefinitions");
        if (defs.isArray()) {
            for (JsonNode def : defs) {
                String eventType = def.path("eventType").asText(null);
                if (eventType == null || eventType.isBlank()) {
                    continue;
                }
                String key = normalizeEventType(eventType);
                types.add(key);
                Set<String> fields = new LinkedHashSet<>();
                JsonNode coreFields = def.path("coreFields");
                if (coreFields.isArray()) {
                    for (JsonNode cf : coreFields) {
                        String name = cf.path("name").asText(null);
                        if (name == null || name.isBlank()) {
                            continue;
                        }
                        String norm = normalizeFieldName(name);
                        if (!SYSTEM_FIELDS.contains(norm)) {
                            fields.add(norm);
                        }
                    }
                }
                fieldsByEvent.put(key, fields);
            }
        }

        if (types.isEmpty()) {
            JsonNode std = es.path("standardFields");
            if (std.isArray() && !std.isEmpty()) {
                types.add("PURCHASE");
                fieldsByEvent.put("PURCHASE", Set.of("amount"));
            }
        }

        return new ReferralProgrammeEventSchemaCatalog(types, fieldsByEvent);
    }

    public Set<String> getEventTypes() {
        return eventTypes;
    }

    public boolean hasEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return false;
        }
        return eventTypes.contains(normalizeEventType(eventType));
    }

    public boolean isPurchaseEngineKey(String criteriaKey) {
        return criteriaKey != null && PURCHASE_ENGINE_KEYS.contains(criteriaKey.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isAllowedCriteriaKey(ReferralRuleTrigger trigger, String eventType, String criteriaKey) {
        if (criteriaKey == null || criteriaKey.isBlank()) {
            return true;
        }
        if (trigger == ReferralRuleTrigger.LINK) {
            return false;
        }
        String key = criteriaKey.trim().toLowerCase(Locale.ROOT);
        if (trigger == ReferralRuleTrigger.PURCHASE && PURCHASE_ENGINE_KEYS.contains(key)) {
            return true;
        }
        String et = resolveEventTypeForRule(trigger, eventType);
        if (et == null) {
            return false;
        }
        Set<String> allowed = filterableFieldsByEvent.getOrDefault(et, Set.of());
        String schemaField = FIRST_CLASS_TO_SCHEMA.getOrDefault(key, key);
        if (allowed.contains(schemaField)) {
            return true;
        }
        if ("amount".equals(schemaField) && "minspend".equals(key) && trigger == ReferralRuleTrigger.PURCHASE) {
            return allowed.contains("amount");
        }
        return false;
    }

    public boolean isAllowedMetadataKey(ReferralRuleTrigger trigger, String eventType, String metadataKey) {
        if (metadataKey == null || metadataKey.isBlank() || trigger == ReferralRuleTrigger.LINK) {
            return false;
        }
        String et = resolveEventTypeForRule(trigger, eventType);
        if (et == null) {
            return false;
        }
        Set<String> allowed = filterableFieldsByEvent.getOrDefault(et, Set.of());
        return allowed.contains(normalizeFieldName(metadataKey));
    }

    public Map<String, List<ReferralRuleSchemaResponse.CriteriaField>> toCriteriaFieldsByEvent() {
        Map<String, List<ReferralRuleSchemaResponse.CriteriaField>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : filterableFieldsByEvent.entrySet()) {
            List<ReferralRuleSchemaResponse.CriteriaField> fields = new ArrayList<>();
            boolean purchase = "PURCHASE".equals(e.getKey());
            if (purchase) {
                fields.add(criteriaField("minPurchaseCount", "Minimum purchases", "number", true));
                fields.add(criteriaField("minSpend", "Minimum total spend", "number", true));
                fields.add(criteriaField("windowDays", "Time window (days)", "number", true));
                fields.add(criteriaField("firstPurchaseOnly", "First purchase only", "boolean", true));
            }
            for (String name : e.getValue()) {
                if ("amount".equals(name) && purchase) {
                    continue;
                }
                String storageKey = mapToStorageKey(name, purchase);
                fields.add(criteriaField(storageKey, humanize(name), guessType(name), purchase));
            }
            out.put(e.getKey(), fields);
        }
        return out;
    }

    private static String mapToStorageKey(String schemaField, boolean purchase) {
        return switch (schemaField) {
            case "merchantid", "merchant_id" -> "merchantId";
            case "category" -> "category";
            case "channel" -> "channel";
            case "sku" -> "sku";
            case "region", "geo", "country" -> "region";
            case "amount" -> purchase ? "minSpend" : "meta.amount";
            default -> "meta." + schemaField;
        };
    }

    private static ReferralRuleSchemaResponse.CriteriaField criteriaField(
        String key,
        String label,
        String type,
        boolean purchaseOnly
    ) {
        ReferralRuleSchemaResponse.CriteriaField f = new ReferralRuleSchemaResponse.CriteriaField();
        f.setKey(key);
        f.setLabel(label);
        f.setType(type);
        f.setPurchaseOnly(purchaseOnly);
        return f;
    }

    private static String guessType(String name) {
        if ("amount".equals(name)) {
            return "number";
        }
        return "string";
    }

    private static String humanize(String name) {
        String spaced = name.replace('_', ' ');
        if (spaced.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String resolveEventTypeForRule(ReferralRuleTrigger trigger, String eventType) {
        if (trigger == ReferralRuleTrigger.PURCHASE) {
            return "PURCHASE";
        }
        if (trigger == ReferralRuleTrigger.INTEGRATION_EVENT && eventType != null && !eventType.isBlank()) {
            return normalizeEventType(eventType);
        }
        return null;
    }

    private static String normalizeEventType(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeFieldName(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }
}
