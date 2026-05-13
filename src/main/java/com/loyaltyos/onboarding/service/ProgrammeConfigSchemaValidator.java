package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ProgrammeConfigSchemaValidator {

    private static final String SCHEMA_PATH = "schemas/programme-config.schema.json";

    private final ObjectMapper objectMapper;

    private volatile JsonSchema cachedSchema;

    public ProgrammeConfigSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void validate(JsonNode config) {
        if (config == null || config.isNull()) {
            throw new ProgrammeConfigValidationException("Programme config is required", Map.of("programmeConfig", "Missing config"));
        }

        JsonSchema schema = schema();
        Set<ValidationMessage> errors = schema.validate(config);
        if (errors == null || errors.isEmpty()) return;

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ValidationMessage vm : errors) {
            // Use instance location (JSON pointer) for stable frontend mapping.
            String path = "programmeConfig";
            try {
                Object loc = vm.getInstanceLocation();
                if (loc != null) {
                    String s = String.valueOf(loc);
                    if (!s.isBlank() && !s.equals("#")) path = s;
                }
            } catch (Exception ignored) {
                // Fall back to a generic key
            }
            fieldErrors.put(path, vm.getMessage());
        }
        throw new ProgrammeConfigValidationException("Programme configuration validation failed", fieldErrors);
    }

    private JsonSchema schema() {
        JsonSchema s = cachedSchema;
        if (s != null) return s;
        synchronized (this) {
            if (cachedSchema != null) return cachedSchema;
            try (InputStream is = new ClassPathResource(SCHEMA_PATH).getInputStream()) {
                JsonNode schemaNode = objectMapper.readTree(is);
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                cachedSchema = factory.getSchema(schemaNode);
                return cachedSchema;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load programme config JSON schema from " + SCHEMA_PATH, e);
            }
        }
    }
}

