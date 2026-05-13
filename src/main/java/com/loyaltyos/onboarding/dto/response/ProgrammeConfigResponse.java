package com.loyaltyos.onboarding.dto.response;

import java.util.List;
import java.util.Map;

public class ProgrammeConfigResponse {
    private String tenantId;
    private String programmeName;
    private String pointsName;
    private String pointsSymbol;
    private String baseCurrency;
    private String basePointsRate;
    private String minRedemptionPoints;
    private String maxRedemptionPctPerTxn;
    private Boolean tiersEnabled;
    private List<Tier> tiers;
    private Map<String, Object> webhookConfig;

    public static class Tier {
        private String tierUid;
        private String name;
        private Integer rank;
        private String entryThreshold;
        private String maintenanceThreshold;
        private String thresholdType;
        private String multiplier;
        private List<String> benefits;

        public Tier() {}

        public Tier(
            String tierUid,
            String name,
            Integer rank,
            String entryThreshold,
            String maintenanceThreshold,
            String thresholdType,
            String multiplier,
            List<String> benefits
        ) {
            this.tierUid = tierUid;
            this.name = name;
            this.rank = rank;
            this.entryThreshold = entryThreshold;
            this.maintenanceThreshold = maintenanceThreshold;
            this.thresholdType = thresholdType;
            this.multiplier = multiplier;
            this.benefits = benefits;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String tierUid;
            private String name;
            private Integer rank;
            private String entryThreshold;
            private String maintenanceThreshold;
            private String thresholdType;
            private String multiplier;
            private List<String> benefits;

            private Builder() {}

            public Builder tierUid(String tierUid) { this.tierUid = tierUid; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder rank(Integer rank) { this.rank = rank; return this; }
            public Builder entryThreshold(String entryThreshold) { this.entryThreshold = entryThreshold; return this; }
            public Builder maintenanceThreshold(String maintenanceThreshold) { this.maintenanceThreshold = maintenanceThreshold; return this; }
            public Builder thresholdType(String thresholdType) { this.thresholdType = thresholdType; return this; }
            public Builder multiplier(String multiplier) { this.multiplier = multiplier; return this; }
            public Builder benefits(List<String> benefits) { this.benefits = benefits; return this; }

            public Tier build() {
                return new Tier(tierUid, name, rank, entryThreshold, maintenanceThreshold, thresholdType, multiplier, benefits);
            }
        }

        public String getTierUid() { return tierUid; }
        public String getName() { return name; }
        public Integer getRank() { return rank; }
        public String getEntryThreshold() { return entryThreshold; }
        public String getMaintenanceThreshold() { return maintenanceThreshold; }
        public String getThresholdType() { return thresholdType; }
        public String getMultiplier() { return multiplier; }
        public List<String> getBenefits() { return benefits; }
    }

    public ProgrammeConfigResponse() {}

    public ProgrammeConfigResponse(
        String tenantId,
        String programmeName,
        String pointsName,
        String pointsSymbol,
        String baseCurrency,
        String basePointsRate,
        String minRedemptionPoints,
        String maxRedemptionPctPerTxn,
        Boolean tiersEnabled,
        List<Tier> tiers,
        Map<String, Object> webhookConfig
    ) {
        this.tenantId = tenantId;
        this.programmeName = programmeName;
        this.pointsName = pointsName;
        this.pointsSymbol = pointsSymbol;
        this.baseCurrency = baseCurrency;
        this.basePointsRate = basePointsRate;
        this.minRedemptionPoints = minRedemptionPoints;
        this.maxRedemptionPctPerTxn = maxRedemptionPctPerTxn;
        this.tiersEnabled = tiersEnabled;
        this.tiers = tiers;
        this.webhookConfig = webhookConfig;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String programmeName;
        private String pointsName;
        private String pointsSymbol;
        private String baseCurrency;
        private String basePointsRate;
        private String minRedemptionPoints;
        private String maxRedemptionPctPerTxn;
        private Boolean tiersEnabled;
        private List<Tier> tiers;
        private Map<String, Object> webhookConfig;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeName(String programmeName) { this.programmeName = programmeName; return this; }
        public Builder pointsName(String pointsName) { this.pointsName = pointsName; return this; }
        public Builder pointsSymbol(String pointsSymbol) { this.pointsSymbol = pointsSymbol; return this; }
        public Builder baseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; return this; }
        public Builder basePointsRate(String basePointsRate) { this.basePointsRate = basePointsRate; return this; }
        public Builder minRedemptionPoints(String minRedemptionPoints) { this.minRedemptionPoints = minRedemptionPoints; return this; }
        public Builder maxRedemptionPctPerTxn(String maxRedemptionPctPerTxn) { this.maxRedemptionPctPerTxn = maxRedemptionPctPerTxn; return this; }
        public Builder tiersEnabled(Boolean tiersEnabled) { this.tiersEnabled = tiersEnabled; return this; }
        public Builder tiers(List<Tier> tiers) { this.tiers = tiers; return this; }
        public Builder webhookConfig(Map<String, Object> webhookConfig) { this.webhookConfig = webhookConfig; return this; }

        public ProgrammeConfigResponse build() {
            return new ProgrammeConfigResponse(
                tenantId,
                programmeName,
                pointsName,
                pointsSymbol,
                baseCurrency,
                basePointsRate,
                minRedemptionPoints,
                maxRedemptionPctPerTxn,
                tiersEnabled,
                tiers,
                webhookConfig
            );
        }
    }

    public String getTenantId() { return tenantId; }
    public String getProgrammeName() { return programmeName; }
    public String getPointsName() { return pointsName; }
    public String getPointsSymbol() { return pointsSymbol; }
    public String getBaseCurrency() { return baseCurrency; }
    public String getBasePointsRate() { return basePointsRate; }
    public String getMinRedemptionPoints() { return minRedemptionPoints; }
    public String getMaxRedemptionPctPerTxn() { return maxRedemptionPctPerTxn; }
    public Boolean getTiersEnabled() { return tiersEnabled; }
    public List<Tier> getTiers() { return tiers; }
    public Map<String, Object> getWebhookConfig() { return webhookConfig; }
}

