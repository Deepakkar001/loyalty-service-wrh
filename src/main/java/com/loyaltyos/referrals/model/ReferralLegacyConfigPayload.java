package com.loyaltyos.referrals.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deserializes deprecated programme config fields (write-only on {@link ReferralProgrammeConfig}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferralLegacyConfigPayload {

    private String key;
    private String label;
    private String description;
    /** Legacy evaluator name: SIGNUP, FIRST_PURCHASE, NTH_PURCHASE, SPEND_THRESHOLD */
    private String evaluator;
    private Map<String, Object> condition;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(String evaluator) {
        this.evaluator = evaluator;
    }

    public Map<String, Object> getCondition() {
        return condition;
    }

    public void setCondition(Map<String, Object> condition) {
        this.condition = condition;
    }

    public static List<String> copyEnabledKeys(List<String> enabled) {
        if (enabled == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String raw : enabled) {
            if (raw != null && !raw.isBlank()) {
                out.add(raw.trim());
            }
        }
        return out;
    }
}
