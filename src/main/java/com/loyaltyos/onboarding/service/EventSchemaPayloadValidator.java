package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates an incoming event JSON map against the programme's {@code eventSchema}.
 * Supports {@code eventDefinitions} (per eventType core fields) with legacy fallback to {@code standardFields}.
 * <p>Checks {@code required} flags and {@code type} for each defined field ({@code string}, {@code number},
 * {@code integer}, {@code boolean}, {@code date-time}, {@code object}). Types are enforced when a value is present;
 * unknown or blank {@code type} defaults to {@code string}.</p>
 */
public final class EventSchemaPayloadValidator {

    private EventSchemaPayloadValidator() {}

    /**
     * @return field name → error message (empty when valid)
     */
    public static Map<String, String> validatePayload(Map<String, Object> payload, JsonNode programmeRoot) {
        Map<String, String> errors = new LinkedHashMap<>();
        Object rawEt = payload.get("eventType");
        if (rawEt == null || String.valueOf(rawEt).isBlank()) {
            errors.put("eventType", "eventType is required on every event");
            return errors;
        }
        String eventType = String.valueOf(rawEt).trim();
        JsonNode es = programmeRoot.path("eventSchema");
        if (es.isMissingNode() || es.isNull()) {
            return errors;
        }

        JsonNode defs = es.path("eventDefinitions");
        if (defs.isArray() && !defs.isEmpty()) {
            JsonNode def = findEventDefinition(defs, eventType);
            if (def == null || def.isMissingNode()) {
                errors.put("eventType", "No configured schema for eventType \"" + eventType + "\"");
                return errors;
            }
            mergeFieldErrors(errors, schemaFieldErrors(def.path("coreFields"), payload));
            mergeFieldErrors(errors, schemaFieldErrors(es.path("customFields"), payload));
        } else {
            mergeFieldErrors(errors, schemaFieldErrors(es.path("standardFields"), payload));
            mergeFieldErrors(errors, schemaFieldErrors(es.path("customFields"), payload));
        }
        return errors;
    }

    private static JsonNode findEventDefinition(JsonNode defs, String eventType) {
        for (JsonNode d : defs) {
            String configured = d.path("eventType").asText("").trim();
            if (!configured.isEmpty() && configured.equalsIgnoreCase(eventType)) {
                return d;
            }
        }
        return null;
    }

    private static Map<String, String> schemaFieldErrors(JsonNode fieldsArr, Map<String, Object> payload) {
        Map<String, String> e = new LinkedHashMap<>();
        if (!fieldsArr.isArray()) {
            return e;
        }
        for (JsonNode f : fieldsArr) {
            String name = f.path("name").asText("").trim();
            if (name.isEmpty()) {
                continue;
            }
            boolean required = f.path("required").asBoolean(false);
            String type = normalizeType(f.path("type").asText(""));

            boolean present = payload.containsKey(name);
            Object value = present ? payload.get(name) : null;

            if (required && (!present || value == null || isBlankValue(value))) {
                e.putIfAbsent(name, name + " is required for this event schema");
                continue;
            }
            if (!present || value == null) {
                continue;
            }
            if (isBlankValue(value) && required) {
                e.putIfAbsent(name, name + " is required for this event schema");
                continue;
            }
            if (isBlankValue(value)) {
                continue;
            }

            String typeError = typeMismatchMessage(name, type, value);
            if (typeError != null) {
                e.putIfAbsent(name, typeError);
            }
        }
        return e;
    }

    private static String normalizeType(String raw) {
        if (raw == null) {
            return "string";
        }
        String t = raw.trim().toLowerCase();
        return switch (t) {
            case "number", "integer", "boolean", "date-time", "object" -> t;
            default -> "string";
        };
    }

    private static boolean isBlankValue(Object value) {
        return value instanceof String s && s.isBlank();
    }

    private static String typeMismatchMessage(String name, String type, Object value) {
        return switch (type) {
            case "number" -> matchesNumber(value) ? null : name + " must be a number";
            case "integer" -> matchesInteger(value) ? null : name + " must be an integer";
            case "boolean" -> value instanceof Boolean ? null : name + " must be a boolean";
            case "date-time" -> matchesDateTime(value) ? null : name + " must be a date-time string (ISO-8601)";
            case "object" -> matchesObject(value) ? null : name + " must be a JSON object";
            default -> matchesString(value) ? null : name + " must be a string";
        };
    }

    private static boolean matchesString(Object value) {
        return value instanceof String;
    }

    private static boolean matchesNumber(Object value) {
        return value instanceof Number;
    }

    private static boolean matchesInteger(Object value) {
        return value instanceof Number n && isWholeNumber(n);
    }

    private static boolean isWholeNumber(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
            return true;
        }
        BigDecimal bd = new BigDecimal(n.toString());
        return bd.stripTrailingZeros().scale() <= 0;
    }

    private static boolean matchesDateTime(Object value) {
        if (!(value instanceof String s) || s.isBlank()) {
            return false;
        }
        String trimmed = s.trim();
        try {
            Instant.parse(trimmed);
            return true;
        } catch (DateTimeParseException ignored) {
            // try offset datetime
        }
        try {
            java.time.OffsetDateTime.parse(trimmed);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    private static boolean matchesObject(Object value) {
        if (value instanceof Map<?, ?>) {
            return true;
        }
        return value != null && !(value instanceof Collection<?>) && !(value.getClass().isArray());
    }

    private static void mergeFieldErrors(Map<String, String> dest, Map<String, String> src) {
        for (Map.Entry<String, String> en : src.entrySet()) {
            dest.putIfAbsent(en.getKey(), en.getValue());
        }
    }
}
