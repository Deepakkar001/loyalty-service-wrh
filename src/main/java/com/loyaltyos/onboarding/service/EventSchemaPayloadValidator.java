package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates an incoming event JSON map against the programme's {@code eventSchema}.
 * Supports {@code eventDefinitions} (per eventType core fields) with legacy fallback to {@code standardFields}.
 * <p><b>Persistence of {@code required}.</b> Field definitions live in MySQL {@code programme_config.config_json}
 * under {@code eventSchema}, for example {@code eventDefinitions[].coreFields[]} and {@code customFields[]}, each with
 * {@code name}, {@code type}, and {@code required}. Values are written when tenants save configuration through
 * {@code PUT /api/v2/programmes/{uid}/config} (see {@link com.loyaltyos.onboarding.service.ProgrammeService#saveConfig}).
 * This class only reads the stored booleans: for every field where {@code required} is true, the payload must include
 * that property or validation returns an error map used by sandbox validation and integration flows.</p>
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
            mergeFieldErrors(errors, requiredFieldErrors(def.path("coreFields"), payload));
            mergeFieldErrors(errors, requiredFieldErrors(es.path("customFields"), payload));
        } else {
            mergeFieldErrors(errors, requiredFieldErrors(es.path("standardFields"), payload));
            mergeFieldErrors(errors, requiredFieldErrors(es.path("customFields"), payload));
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

    private static Map<String, String> requiredFieldErrors(JsonNode fieldsArr, Map<String, Object> payload) {
        Map<String, String> e = new LinkedHashMap<>();
        if (!fieldsArr.isArray()) {
            return e;
        }
        for (JsonNode f : fieldsArr) {
            if (!f.path("required").asBoolean(false)) {
                continue;
            }
            String name = f.path("name").asText("").trim();
            if (name.isEmpty()) {
                continue;
            }
            if (!payload.containsKey(name) || payload.get(name) == null) {
                e.putIfAbsent(name, name + " is required for this event schema");
            }
        }
        return e;
    }

    private static void mergeFieldErrors(Map<String, String> dest, Map<String, String> src) {
        for (Map.Entry<String, String> en : src.entrySet()) {
            dest.putIfAbsent(en.getKey(), en.getValue());
        }
    }
}
