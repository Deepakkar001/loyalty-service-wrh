package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Legacy-compatible request shape used by existing frontend Step4Programme.tsx.
 * This is intentionally kept close to the current UI payload.
 */
public class ProgrammeConfigRequest {

    @NotBlank
    private String programmeName;

    @NotBlank
    private String pointsName;

    @NotBlank
    private String pointsSymbol;

    @NotBlank
    private String baseCurrency;

    @NotNull
    @Positive
    private BigDecimal basePointsRate;

    @NotNull
    @PositiveOrZero
    private BigDecimal minRedemptionPoints;

    @NotNull
    @PositiveOrZero
    private BigDecimal maxRedemptionPctPerTxn;

    @NotNull
    private Boolean tiersEnabled;

    @NotNull
    @Valid
    private List<TierRequest> tiers;

    private Map<String, Object> notificationPreferences;

    private Map<String, Object> webhookConfig;

    public static class TierRequest {
        @NotBlank
        private String name;

        @NotNull
        private Integer rank;

        @NotNull
        @PositiveOrZero
        private BigDecimal minPoints;

        // unused by backend for now, but accepted for compatibility
        private BigDecimal maxPoints;

        @NotNull
        @Positive
        private BigDecimal multiplier;

        private List<String> benefits;

        public TierRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public BigDecimal getMinPoints() { return minPoints; }
        public void setMinPoints(BigDecimal minPoints) { this.minPoints = minPoints; }
        public BigDecimal getMaxPoints() { return maxPoints; }
        public void setMaxPoints(BigDecimal maxPoints) { this.maxPoints = maxPoints; }
        public BigDecimal getMultiplier() { return multiplier; }
        public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
        public List<String> getBenefits() { return benefits; }
        public void setBenefits(List<String> benefits) { this.benefits = benefits; }
    }

    public ProgrammeConfigRequest() {}

    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
    public String getPointsName() { return pointsName; }
    public void setPointsName(String pointsName) { this.pointsName = pointsName; }
    public String getPointsSymbol() { return pointsSymbol; }
    public void setPointsSymbol(String pointsSymbol) { this.pointsSymbol = pointsSymbol; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public BigDecimal getBasePointsRate() { return basePointsRate; }
    public void setBasePointsRate(BigDecimal basePointsRate) { this.basePointsRate = basePointsRate; }
    public BigDecimal getMinRedemptionPoints() { return minRedemptionPoints; }
    public void setMinRedemptionPoints(BigDecimal minRedemptionPoints) { this.minRedemptionPoints = minRedemptionPoints; }
    public BigDecimal getMaxRedemptionPctPerTxn() { return maxRedemptionPctPerTxn; }
    public void setMaxRedemptionPctPerTxn(BigDecimal maxRedemptionPctPerTxn) { this.maxRedemptionPctPerTxn = maxRedemptionPctPerTxn; }
    public Boolean getTiersEnabled() { return tiersEnabled; }
    public void setTiersEnabled(Boolean tiersEnabled) { this.tiersEnabled = tiersEnabled; }
    public List<TierRequest> getTiers() { return tiers; }
    public void setTiers(List<TierRequest> tiers) { this.tiers = tiers; }
    public Map<String, Object> getNotificationPreferences() { return notificationPreferences; }
    public void setNotificationPreferences(Map<String, Object> notificationPreferences) { this.notificationPreferences = notificationPreferences; }
    public Map<String, Object> getWebhookConfig() { return webhookConfig; }
    public void setWebhookConfig(Map<String, Object> webhookConfig) { this.webhookConfig = webhookConfig; }
}

