package com.loyaltyos.referrals.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReferralRuleSchemaResponse {

    private List<TriggerOption> triggers = new ArrayList<>();
    private List<CriteriaField> criteriaFields = new ArrayList<>();
    private List<RuleTemplate> templates = new ArrayList<>();
    private String programmeUid;
    private List<String> programmeEventTypes = new ArrayList<>();
    private Map<String, List<CriteriaField>> criteriaFieldsByEvent = new LinkedHashMap<>();

    public List<TriggerOption> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<TriggerOption> triggers) {
        this.triggers = triggers != null ? triggers : new ArrayList<>();
    }

    public List<CriteriaField> getCriteriaFields() {
        return criteriaFields;
    }

    public void setCriteriaFields(List<CriteriaField> criteriaFields) {
        this.criteriaFields = criteriaFields != null ? criteriaFields : new ArrayList<>();
    }

    public List<RuleTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<RuleTemplate> templates) {
        this.templates = templates != null ? templates : new ArrayList<>();
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public List<String> getProgrammeEventTypes() {
        return programmeEventTypes;
    }

    public void setProgrammeEventTypes(List<String> programmeEventTypes) {
        this.programmeEventTypes = programmeEventTypes != null ? programmeEventTypes : new ArrayList<>();
    }

    public Map<String, List<CriteriaField>> getCriteriaFieldsByEvent() {
        return criteriaFieldsByEvent;
    }

    public void setCriteriaFieldsByEvent(Map<String, List<CriteriaField>> criteriaFieldsByEvent) {
        this.criteriaFieldsByEvent = criteriaFieldsByEvent != null ? criteriaFieldsByEvent : new LinkedHashMap<>();
    }

    public static class TriggerOption {
        private String value;
        private String label;
        private String description;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
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
    }

    public static class CriteriaField {
        private String key;
        private String label;
        private String type;
        private String hint;
        private boolean purchaseOnly;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }

        public boolean isPurchaseOnly() {
            return purchaseOnly;
        }

        public void setPurchaseOnly(boolean purchaseOnly) {
            this.purchaseOnly = purchaseOnly;
        }
    }

    public static class RuleTemplate {
        private String key;
        private String label;
        private String description;
        private String trigger;
        private java.util.Map<String, Object> criteriaDefaults;

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

        public String getTrigger() {
            return trigger;
        }

        public void setTrigger(String trigger) {
            this.trigger = trigger;
        }

        public java.util.Map<String, Object> getCriteriaDefaults() {
            return criteriaDefaults;
        }

        public void setCriteriaDefaults(java.util.Map<String, Object> criteriaDefaults) {
            this.criteriaDefaults = criteriaDefaults;
        }
    }
}
