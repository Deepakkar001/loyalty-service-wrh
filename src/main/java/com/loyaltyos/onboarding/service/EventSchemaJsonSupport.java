package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.campaigns.util.TriggerEventTypes;
import com.loyaltyos.onboarding.dto.EventDefinitionRequest;
import com.loyaltyos.onboarding.dto.EventSchemaSettingsPatchRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared helpers for programme {@code config_json.eventSchema} and campaign {@code event_schema} JSON.
 */
public final class EventSchemaJsonSupport {

    private EventSchemaJsonSupport() {}

    /**
     * Property names allowed under {@code event.*} for a specific trigger event type.
     * Includes programme-wide customFields when present. Empty set = no enforcement.
     */
    public static Set<String> extractEventFieldAllowlistForTrigger(JsonNode eventSchemaRoot, String triggerEventType) {
        Set<String> names = new LinkedHashSet<>();
        if (eventSchemaRoot == null || eventSchemaRoot.isMissingNode() || eventSchemaRoot.isNull()) {
            return Set.copyOf(names);
        }
        String normalizedTrigger = normalizeEventType(triggerEventType);
        JsonNode defs = eventSchemaRoot.path("eventDefinitions");
        if (defs.isArray() && !defs.isEmpty() && !normalizedTrigger.isEmpty()) {
            JsonNode def = findEventDefinition(defs, normalizedTrigger);
            if (def != null && !def.isMissingNode()) {
                collectFieldNames(def.path("coreFields"), names);
            }
        } else {
            for (JsonNode sf : eventSchemaRoot.path("standardFields")) {
                String n = sf.path("name").asText(null);
                if (n != null && !n.isBlank()) {
                    names.add(n.trim());
                }
            }
        }
        for (JsonNode cf : eventSchemaRoot.path("customFields")) {
            String n = cf.path("name").asText(null);
            if (n != null && !n.isBlank()) {
                names.add(n.trim());
            }
        }
        return Set.copyOf(names);
    }

    /** All event types declared in {@code eventDefinitions}. */
    public static List<String> extractEventTypes(JsonNode eventSchemaRoot) {
        List<String> types = new ArrayList<>();
        if (eventSchemaRoot == null || eventSchemaRoot.isMissingNode() || eventSchemaRoot.isNull()) {
            return types;
        }
        JsonNode defs = eventSchemaRoot.path("eventDefinitions");
        if (defs.isArray()) {
            for (JsonNode def : defs) {
                String et = def.path("eventType").asText(null);
                if (et != null && !et.isBlank()) {
                    types.add(et.trim());
                }
            }
        }
        if (types.isEmpty()) {
            JsonNode std = eventSchemaRoot.path("standardFields");
            if (std.isArray() && !std.isEmpty()) {
                types.add("PURCHASE");
            }
        }
        return types;
    }

    /** Comma-separated trigger list derived from {@code eventDefinitions[].eventType}. */
    public static String triggerTypesFromEventSchema(JsonNode eventSchemaRoot) {
        return TriggerEventTypes.normalize(String.join(",", extractEventTypes(eventSchemaRoot)));
    }

    /**
     * Validates event schema document shape. Returns error message or null when valid.
     */
    public static String validateEventSchemaDocument(JsonNode eventSchemaRoot) {
        if (eventSchemaRoot == null || eventSchemaRoot.isMissingNode() || eventSchemaRoot.isNull()) {
            return "eventSchema is required";
        }
        JsonNode defs = eventSchemaRoot.path("eventDefinitions");
        if (!defs.isArray() || defs.isEmpty()) {
            return "At least one event definition is required";
        }
        Set<String> eventTypes = new LinkedHashSet<>();
        for (JsonNode def : defs) {
            String eventType = def.path("eventType").asText("").trim();
            if (eventType.isEmpty()) {
                return "Each event definition needs an eventType";
            }
            if (!eventType.matches("^[A-Za-z0-9][A-Za-z0-9._-]*$")) {
                return "Invalid eventType: " + eventType;
            }
            String key = eventType.toUpperCase(Locale.ROOT);
            if (!eventTypes.add(key)) {
                return "Duplicate eventType: " + eventType;
            }
            JsonNode coreFields = def.path("coreFields");
            if (!coreFields.isArray() || coreFields.isEmpty()) {
                return "Add at least one core field for " + eventType;
            }
            Set<String> fieldNames = new LinkedHashSet<>();
            for (JsonNode f : coreFields) {
                String name = f.path("name").asText("").trim();
                if (name.isEmpty()) {
                    return "Core field names cannot be empty";
                }
                if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                    return "Invalid core field name: " + name;
                }
                if (!fieldNames.add(name)) {
                    return "Duplicate field '" + name + "' in event " + eventType;
                }
            }
        }
        JsonNode customFields = eventSchemaRoot.path("customFields");
        if (customFields.isArray()) {
            Set<String> customNames = new LinkedHashSet<>();
            for (JsonNode c : customFields) {
                String name = c.path("name").asText("").trim();
                if (name.isEmpty()) {
                    return "Custom field names cannot be empty";
                }
                if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                    return "Invalid custom field name: " + name;
                }
                if (!customNames.add(name)) {
                    return "Duplicate custom field: " + name;
                }
            }
        }
        JsonNode bcd = eventSchemaRoot.path("backwardCompatibilityDays");
        if (bcd.isNumber()) {
            int days = bcd.intValue();
            if (days < 0 || days > 365) {
                return "backwardCompatibilityDays must be between 0 and 365";
            }
        }
        return null;
    }

    public static JsonNode findEventDefinition(JsonNode defs, String eventType) {
        if (defs == null || !defs.isArray() || eventType == null || eventType.isBlank()) {
            return null;
        }
        for (JsonNode d : defs) {
            String configured = d.path("eventType").asText("").trim();
            if (!configured.isEmpty() && configured.equalsIgnoreCase(eventType.trim())) {
                return d;
            }
        }
        return null;
    }

    private static void collectFieldNames(JsonNode fieldsArr, Set<String> names) {
        if (!fieldsArr.isArray()) {
            return;
        }
        for (JsonNode f : fieldsArr) {
            String n = f.path("name").asText(null);
            if (n != null && !n.isBlank()) {
                names.add(n.trim());
            }
        }
    }

    private static String normalizeEventType(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    /** Validates a single event definition (without duplicate checks against siblings). */
    public static String validateEventDefinition(JsonNode def) {
        if (def == null || def.isMissingNode() || def.isNull()) {
            return "Event definition is required";
        }
        String eventType = def.path("eventType").asText("").trim();
        if (eventType.isEmpty()) {
            return "Each event definition needs an eventType";
        }
        if (!eventType.matches("^[A-Za-z0-9][A-Za-z0-9._-]*$")) {
            return "Invalid eventType: " + eventType;
        }
        JsonNode coreFields = def.path("coreFields");
        if (!coreFields.isArray() || coreFields.isEmpty()) {
            return "Add at least one core field for " + eventType;
        }
        Set<String> fieldNames = new LinkedHashSet<>();
        for (JsonNode f : coreFields) {
            String name = f.path("name").asText("").trim();
            if (name.isEmpty()) {
                return "Core field names cannot be empty";
            }
            if (!name.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                return "Invalid core field name: " + name;
            }
            if (!fieldNames.add(name)) {
                return "Duplicate field '" + name + "' in event " + eventType;
            }
        }
        return null;
    }

    public static ObjectNode toEventDefinitionNode(EventDefinitionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Event definition is required");
        }
        ObjectNode def = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        def.put("eventType", request.getEventType().trim());
        def.set("coreFields", request.getCoreFields());
        String err = validateEventDefinition(def);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        return def;
    }

    /**
     * Replaces one event definition matched by {@code pathEventType} (case-insensitive).
     * Recomputes {@code standardFields} and validates the full document.
     */
    public static void replaceEventDefinition(ObjectNode eventSchemaRoot, String pathEventType, ObjectNode updatedDefinition) {
        ArrayNode defs = requireEventDefinitionsArray(eventSchemaRoot);
        int idx = indexOfEventDefinition(defs, pathEventType);
        if (idx < 0) {
            throw new IllegalArgumentException("Event type not found: " + pathEventType);
        }
        String newType = updatedDefinition.path("eventType").asText("").trim();
        for (int i = 0; i < defs.size(); i++) {
            if (i == idx) {
                continue;
            }
            String other = defs.get(i).path("eventType").asText("").trim();
            if (!other.isEmpty() && other.equalsIgnoreCase(newType)) {
                throw new IllegalArgumentException("Duplicate eventType: " + newType);
            }
        }
        defs.set(idx, updatedDefinition);
        finalizeEventSchemaDocument(eventSchemaRoot);
    }

    /** Appends a new event definition. */
    public static void addEventDefinition(ObjectNode eventSchemaRoot, ObjectNode newDefinition) {
        ArrayNode defs = requireEventDefinitionsArray(eventSchemaRoot);
        String newType = newDefinition.path("eventType").asText("").trim();
        if (indexOfEventDefinition(defs, newType) >= 0) {
            throw new IllegalArgumentException("Duplicate eventType: " + newType);
        }
        defs.add(newDefinition);
        finalizeEventSchemaDocument(eventSchemaRoot);
    }

    /** Removes an event definition; at least one must remain. */
    public static void removeEventDefinition(ObjectNode eventSchemaRoot, String pathEventType) {
        ArrayNode defs = requireEventDefinitionsArray(eventSchemaRoot);
        if (defs.size() <= 1) {
            throw new IllegalArgumentException("At least one event definition is required");
        }
        int idx = indexOfEventDefinition(defs, pathEventType);
        if (idx < 0) {
            throw new IllegalArgumentException("Event type not found: " + pathEventType);
        }
        defs.remove(idx);
        finalizeEventSchemaDocument(eventSchemaRoot);
    }

    /** Patches programme-wide schema settings (version, backwardCompatibilityDays, customFields). */
    public static void applySettingsPatch(ObjectNode eventSchemaRoot, EventSchemaSettingsPatchRequest patch) {
        if (patch == null) {
            return;
        }
        if (patch.getVersion() != null) {
            eventSchemaRoot.put("version", patch.getVersion());
        }
        if (patch.getBackwardCompatibilityDays() != null) {
            eventSchemaRoot.put("backwardCompatibilityDays", patch.getBackwardCompatibilityDays());
        }
        if (patch.getCustomFields() != null) {
            if (!patch.getCustomFields().isArray()) {
                throw new IllegalArgumentException("customFields must be an array");
            }
            eventSchemaRoot.set("customFields", patch.getCustomFields());
        }
        finalizeEventSchemaDocument(eventSchemaRoot);
    }

    private static ArrayNode requireEventDefinitionsArray(ObjectNode eventSchemaRoot) {
        JsonNode defs = eventSchemaRoot.path("eventDefinitions");
        if (defs.isArray()) {
            return (ArrayNode) defs;
        }
        return eventSchemaRoot.putArray("eventDefinitions");
    }

    private static int indexOfEventDefinition(ArrayNode defs, String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return -1;
        }
        for (int i = 0; i < defs.size(); i++) {
            String configured = defs.get(i).path("eventType").asText("").trim();
            if (!configured.isEmpty() && configured.equalsIgnoreCase(eventType.trim())) {
                return i;
            }
        }
        return -1;
    }

    private static void finalizeEventSchemaDocument(ObjectNode eventSchemaRoot) {
        ensureStandardFieldsUnion(eventSchemaRoot);
        String err = validateEventSchemaDocument(eventSchemaRoot);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
    }

    /** Ensures {@code standardFields} union exists for backwards compatibility consumers. */
    public static void ensureStandardFieldsUnion(ObjectNode eventSchemaRoot) {
        if (eventSchemaRoot == null) {
            return;
        }
        JsonNode defs = eventSchemaRoot.path("eventDefinitions");
        if (!defs.isArray()) {
            return;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayNode standard = eventSchemaRoot.putArray("standardFields");
        for (JsonNode def : defs) {
            for (JsonNode cf : def.path("coreFields")) {
                String name = cf.path("name").asText("").trim();
                if (name.isEmpty() || !seen.add(name)) {
                    continue;
                }
                ObjectNode row = standard.addObject();
                row.put("name", name);
                row.put("type", cf.path("type").asText("string"));
                row.put("required", cf.path("required").asBoolean(false));
            }
        }
    }
}
