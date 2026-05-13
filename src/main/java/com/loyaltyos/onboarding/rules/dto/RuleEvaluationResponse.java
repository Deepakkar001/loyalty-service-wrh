package com.loyaltyos.onboarding.rules.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RuleEvaluationResponse {

    private String tenantId;
    private String programmeUid;
    private String customerId;
    private String eventId;

    private BigDecimal basePointsCalculated;
    private BigDecimal tierMultiplier;
    private BigDecimal finalPointsAwarded;

    private BigDecimal dailyCapRemaining;
    private BigDecimal monthlyCapRemaining;

    private List<MatchedRuleInfo> matchedRules = new ArrayList<>();

    private List<SuppressedRuleInfo> suppressedRules = new ArrayList<>();

    private List<RewardCommand> rewardCommands = new ArrayList<>();

    private boolean success;
    private String message;

    /** JSON trace for audit / Finance (also persisted server-side when enabled). */
    private JsonNode evaluationTrace;

    public static class MatchedRuleInfo {
        private String ruleUid;
        private String ruleName;
        private Integer priority;
        private BigDecimal pointsFromThisRule;

        public MatchedRuleInfo() {}

        public MatchedRuleInfo(String ruleUid, String ruleName, Integer priority, BigDecimal pointsFromThisRule) {
            this.ruleUid = ruleUid;
            this.ruleName = ruleName;
            this.priority = priority;
            this.pointsFromThisRule = pointsFromThisRule;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String ruleUid;
            private String ruleName;
            private Integer priority;
            private BigDecimal pointsFromThisRule;

            private Builder() {}

            public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
            public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
            public Builder priority(Integer priority) { this.priority = priority; return this; }
            public Builder pointsFromThisRule(BigDecimal pointsFromThisRule) { this.pointsFromThisRule = pointsFromThisRule; return this; }

            public MatchedRuleInfo build() { return new MatchedRuleInfo(ruleUid, ruleName, priority, pointsFromThisRule); }
        }

        public String getRuleUid() { return ruleUid; }
        public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public BigDecimal getPointsFromThisRule() { return pointsFromThisRule; }
        public void setPointsFromThisRule(BigDecimal pointsFromThisRule) { this.pointsFromThisRule = pointsFromThisRule; }
    }

    public static class SuppressedRuleInfo {
        private String ruleUid;
        private String reason;

        public SuppressedRuleInfo() {}

        public SuppressedRuleInfo(String ruleUid, String reason) {
            this.ruleUid = ruleUid;
            this.reason = reason;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String ruleUid;
            private String reason;

            private Builder() {}

            public Builder ruleUid(String ruleUid) { this.ruleUid = ruleUid; return this; }
            public Builder reason(String reason) { this.reason = reason; return this; }

            public SuppressedRuleInfo build() { return new SuppressedRuleInfo(ruleUid, reason); }
        }

        public String getRuleUid() { return ruleUid; }
        public void setRuleUid(String ruleUid) { this.ruleUid = ruleUid; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RewardCommand {
        private String commandId;
        private String idempotencyKey;
        private String tenantId;
        private String programmeUid;
        private String customerId;
        private String actionType;
        private BigDecimal pointsToAward;
        private String sourceRuleUid;
        private Long timestamp;

        public RewardCommand() {}

        public RewardCommand(
            String commandId,
            String idempotencyKey,
            String tenantId,
            String programmeUid,
            String customerId,
            String actionType,
            BigDecimal pointsToAward,
            String sourceRuleUid,
            Long timestamp
        ) {
            this.commandId = commandId;
            this.idempotencyKey = idempotencyKey;
            this.tenantId = tenantId;
            this.programmeUid = programmeUid;
            this.customerId = customerId;
            this.actionType = actionType;
            this.pointsToAward = pointsToAward;
            this.sourceRuleUid = sourceRuleUid;
            this.timestamp = timestamp;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String commandId;
            private String idempotencyKey;
            private String tenantId;
            private String programmeUid;
            private String customerId;
            private String actionType;
            private BigDecimal pointsToAward;
            private String sourceRuleUid;
            private Long timestamp;

            private Builder() {}

            public Builder commandId(String commandId) { this.commandId = commandId; return this; }
            public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
            public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
            public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
            public Builder customerId(String customerId) { this.customerId = customerId; return this; }
            public Builder actionType(String actionType) { this.actionType = actionType; return this; }
            public Builder pointsToAward(BigDecimal pointsToAward) { this.pointsToAward = pointsToAward; return this; }
            public Builder sourceRuleUid(String sourceRuleUid) { this.sourceRuleUid = sourceRuleUid; return this; }
            public Builder timestamp(Long timestamp) { this.timestamp = timestamp; return this; }

            public RewardCommand build() {
                return new RewardCommand(commandId, idempotencyKey, tenantId, programmeUid, customerId, actionType, pointsToAward, sourceRuleUid, timestamp);
            }
        }

        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getProgrammeUid() { return programmeUid; }
        public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
        public BigDecimal getPointsToAward() { return pointsToAward; }
        public void setPointsToAward(BigDecimal pointsToAward) { this.pointsToAward = pointsToAward; }
        public String getSourceRuleUid() { return sourceRuleUid; }
        public void setSourceRuleUid(String sourceRuleUid) { this.sourceRuleUid = sourceRuleUid; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public RuleEvaluationResponse() {}

    public RuleEvaluationResponse(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        BigDecimal basePointsCalculated,
        BigDecimal tierMultiplier,
        BigDecimal finalPointsAwarded,
        BigDecimal dailyCapRemaining,
        BigDecimal monthlyCapRemaining,
        List<MatchedRuleInfo> matchedRules,
        List<SuppressedRuleInfo> suppressedRules,
        List<RewardCommand> rewardCommands,
        boolean success,
        String message,
        JsonNode evaluationTrace
    ) {
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.customerId = customerId;
        this.eventId = eventId;
        this.basePointsCalculated = basePointsCalculated;
        this.tierMultiplier = tierMultiplier;
        this.finalPointsAwarded = finalPointsAwarded;
        this.dailyCapRemaining = dailyCapRemaining;
        this.monthlyCapRemaining = monthlyCapRemaining;
        this.matchedRules = matchedRules != null ? matchedRules : new ArrayList<>();
        this.suppressedRules = suppressedRules != null ? suppressedRules : new ArrayList<>();
        this.rewardCommands = rewardCommands != null ? rewardCommands : new ArrayList<>();
        this.success = success;
        this.message = message;
        this.evaluationTrace = evaluationTrace;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String programmeUid;
        private String customerId;
        private String eventId;
        private BigDecimal basePointsCalculated;
        private BigDecimal tierMultiplier;
        private BigDecimal finalPointsAwarded;
        private BigDecimal dailyCapRemaining;
        private BigDecimal monthlyCapRemaining;
        private List<MatchedRuleInfo> matchedRules = new ArrayList<>();
        private List<SuppressedRuleInfo> suppressedRules = new ArrayList<>();
        private List<RewardCommand> rewardCommands = new ArrayList<>();
        private boolean success;
        private String message;
        private JsonNode evaluationTrace;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder basePointsCalculated(BigDecimal basePointsCalculated) { this.basePointsCalculated = basePointsCalculated; return this; }
        public Builder tierMultiplier(BigDecimal tierMultiplier) { this.tierMultiplier = tierMultiplier; return this; }
        public Builder finalPointsAwarded(BigDecimal finalPointsAwarded) { this.finalPointsAwarded = finalPointsAwarded; return this; }
        public Builder dailyCapRemaining(BigDecimal dailyCapRemaining) { this.dailyCapRemaining = dailyCapRemaining; return this; }
        public Builder monthlyCapRemaining(BigDecimal monthlyCapRemaining) { this.monthlyCapRemaining = monthlyCapRemaining; return this; }
        public Builder matchedRules(List<MatchedRuleInfo> matchedRules) { this.matchedRules = matchedRules != null ? matchedRules : new ArrayList<>(); return this; }
        public Builder suppressedRules(List<SuppressedRuleInfo> suppressedRules) { this.suppressedRules = suppressedRules != null ? suppressedRules : new ArrayList<>(); return this; }
        public Builder rewardCommands(List<RewardCommand> rewardCommands) { this.rewardCommands = rewardCommands != null ? rewardCommands : new ArrayList<>(); return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder evaluationTrace(JsonNode evaluationTrace) { this.evaluationTrace = evaluationTrace; return this; }

        public RuleEvaluationResponse build() {
            return new RuleEvaluationResponse(
                tenantId, programmeUid, customerId, eventId,
                basePointsCalculated, tierMultiplier, finalPointsAwarded,
                dailyCapRemaining, monthlyCapRemaining,
                matchedRules, suppressedRules, rewardCommands,
                success, message, evaluationTrace
            );
        }
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BigDecimal getBasePointsCalculated() { return basePointsCalculated; }
    public void setBasePointsCalculated(BigDecimal basePointsCalculated) { this.basePointsCalculated = basePointsCalculated; }
    public BigDecimal getTierMultiplier() { return tierMultiplier; }
    public void setTierMultiplier(BigDecimal tierMultiplier) { this.tierMultiplier = tierMultiplier; }
    public BigDecimal getFinalPointsAwarded() { return finalPointsAwarded; }
    public void setFinalPointsAwarded(BigDecimal finalPointsAwarded) { this.finalPointsAwarded = finalPointsAwarded; }
    public BigDecimal getDailyCapRemaining() { return dailyCapRemaining; }
    public void setDailyCapRemaining(BigDecimal dailyCapRemaining) { this.dailyCapRemaining = dailyCapRemaining; }
    public BigDecimal getMonthlyCapRemaining() { return monthlyCapRemaining; }
    public void setMonthlyCapRemaining(BigDecimal monthlyCapRemaining) { this.monthlyCapRemaining = monthlyCapRemaining; }
    public List<MatchedRuleInfo> getMatchedRules() { return matchedRules; }
    public void setMatchedRules(List<MatchedRuleInfo> matchedRules) { this.matchedRules = matchedRules != null ? matchedRules : new ArrayList<>(); }
    public List<SuppressedRuleInfo> getSuppressedRules() { return suppressedRules; }
    public void setSuppressedRules(List<SuppressedRuleInfo> suppressedRules) { this.suppressedRules = suppressedRules != null ? suppressedRules : new ArrayList<>(); }
    public List<RewardCommand> getRewardCommands() { return rewardCommands; }
    public void setRewardCommands(List<RewardCommand> rewardCommands) { this.rewardCommands = rewardCommands != null ? rewardCommands : new ArrayList<>(); }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public JsonNode getEvaluationTrace() { return evaluationTrace; }
    public void setEvaluationTrace(JsonNode evaluationTrace) { this.evaluationTrace = evaluationTrace; }
}
