package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ValidationResponse {

    private String status;
    private String eventId;
    private Instant timestamp;
    private ValidationDetails validation;
    private DryRunResults dryRunResults;
    private String note;

    public static class ValidationDetails {
        private boolean schemaValid;
        private boolean requiredFieldsPresent;
        private boolean customFieldsValid;
        private List<String> errors = new ArrayList<>();

        public boolean isSchemaValid() { return schemaValid; }
        public void setSchemaValid(boolean schemaValid) { this.schemaValid = schemaValid; }
        public boolean isRequiredFieldsPresent() { return requiredFieldsPresent; }
        public void setRequiredFieldsPresent(boolean requiredFieldsPresent) { this.requiredFieldsPresent = requiredFieldsPresent; }
        public boolean isCustomFieldsValid() { return customFieldsValid; }
        public void setCustomFieldsValid(boolean customFieldsValid) { this.customFieldsValid = customFieldsValid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }

    public static class DryRunResults {
        private int rulesEvaluated;
        private int rulesMatched;
        private BigDecimal pointsCalculated;
        private List<EventProcessingResponse.MatchedRuleLine> matchedRules = new ArrayList<>();

        public int getRulesEvaluated() { return rulesEvaluated; }
        public void setRulesEvaluated(int rulesEvaluated) { this.rulesEvaluated = rulesEvaluated; }
        public int getRulesMatched() { return rulesMatched; }
        public void setRulesMatched(int rulesMatched) { this.rulesMatched = rulesMatched; }
        public BigDecimal getPointsCalculated() { return pointsCalculated; }
        public void setPointsCalculated(BigDecimal pointsCalculated) { this.pointsCalculated = pointsCalculated; }
        public List<EventProcessingResponse.MatchedRuleLine> getMatchedRules() { return matchedRules; }
        public void setMatchedRules(List<EventProcessingResponse.MatchedRuleLine> matchedRules) {
            this.matchedRules = matchedRules;
        }
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public ValidationDetails getValidation() { return validation; }
    public void setValidation(ValidationDetails validation) { this.validation = validation; }
    public DryRunResults getDryRunResults() { return dryRunResults; }
    public void setDryRunResults(DryRunResults dryRunResults) { this.dryRunResults = dryRunResults; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
