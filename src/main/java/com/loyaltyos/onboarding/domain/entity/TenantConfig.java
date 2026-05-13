package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import com.loyaltyos.onboarding.domain.enums.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenant_config")
public class TenantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 64)
    private String tenantId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    private SubscriptionTier subscriptionTier = SubscriptionTier.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_modes", nullable = false)
    private IdentityMode ingestionModes = IdentityMode.BOTH;

    @Column(name = "points_currency_rate", nullable = false, precision = 10, scale = 6)
    private java.math.BigDecimal pointsCurrencyRate = new java.math.BigDecimal("0.010000");

    @Column(name = "daily_points_cap", precision = 18, scale = 4)
    private java.math.BigDecimal dailyPointsCap;

    @Column(name = "feature_flags", columnDefinition = "JSON")
    private String featureFlags = "{}";

    @Column(name = "event_schema", columnDefinition = "JSON")
    private String eventSchema;

    @Column(name = "webhook_config", columnDefinition = "JSON")
    private String webhookConfig;

    @Column(name = "branding", columnDefinition = "JSON")
    private String branding;

    @Column(name = "programme_config", columnDefinition = "JSON")
    private String programmeConfig;

    @Column(name = "programme_config_version", nullable = false)
    private Integer programmeConfigVersion = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_residency_region", nullable = false)
    private DataResidencyRegion dataResidencyRegion = DataResidencyRegion.IN;

    @Column(name = "max_active_rules", nullable = false)
    private Integer maxActiveRules = 50;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    /** JPA requires a no-arg constructor. */
    public TenantConfig() {}

    public TenantConfig(
        Long id,
        String tenantId,
        String displayName,
        SubscriptionTier subscriptionTier,
        TenantStatus status,
        IdentityMode ingestionModes,
        java.math.BigDecimal pointsCurrencyRate,
        java.math.BigDecimal dailyPointsCap,
        String featureFlags,
        String eventSchema,
        String webhookConfig,
        String branding,
        String programmeConfig,
        Integer programmeConfigVersion,
        DataResidencyRegion dataResidencyRegion,
        Integer maxActiveRules,
        Instant createdAt,
        Instant updatedAt,
        Integer version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.displayName = displayName;
        this.subscriptionTier = subscriptionTier != null ? subscriptionTier : SubscriptionTier.STANDARD;
        this.status = status != null ? status : TenantStatus.ACTIVE;
        this.ingestionModes = ingestionModes != null ? ingestionModes : IdentityMode.BOTH;
        this.pointsCurrencyRate = pointsCurrencyRate != null ? pointsCurrencyRate : new java.math.BigDecimal("0.010000");
        this.dailyPointsCap = dailyPointsCap;
        this.featureFlags = featureFlags != null ? featureFlags : "{}";
        this.eventSchema = eventSchema;
        this.webhookConfig = webhookConfig;
        this.branding = branding;
        this.programmeConfig = programmeConfig;
        this.programmeConfigVersion = programmeConfigVersion != null ? programmeConfigVersion : 0;
        this.dataResidencyRegion = dataResidencyRegion != null ? dataResidencyRegion : DataResidencyRegion.IN;
        this.maxActiveRules = maxActiveRules != null ? maxActiveRules : 50;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String displayName;
        private SubscriptionTier subscriptionTier = SubscriptionTier.STANDARD;
        private TenantStatus status = TenantStatus.ACTIVE;
        private IdentityMode ingestionModes = IdentityMode.BOTH;
        private java.math.BigDecimal pointsCurrencyRate = new java.math.BigDecimal("0.010000");
        private java.math.BigDecimal dailyPointsCap;
        private String featureFlags = "{}";
        private String eventSchema;
        private String webhookConfig;
        private String branding;
        private String programmeConfig;
        private Integer programmeConfigVersion = 0;
        private DataResidencyRegion dataResidencyRegion = DataResidencyRegion.IN;
        private Integer maxActiveRules = 50;
        private Instant createdAt;
        private Instant updatedAt;
        private Integer version;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }
        public Builder status(TenantStatus status) { this.status = status; return this; }
        public Builder ingestionModes(IdentityMode ingestionModes) { this.ingestionModes = ingestionModes; return this; }
        public Builder pointsCurrencyRate(java.math.BigDecimal pointsCurrencyRate) { this.pointsCurrencyRate = pointsCurrencyRate; return this; }
        public Builder dailyPointsCap(java.math.BigDecimal dailyPointsCap) { this.dailyPointsCap = dailyPointsCap; return this; }
        public Builder featureFlags(String featureFlags) { this.featureFlags = featureFlags; return this; }
        public Builder eventSchema(String eventSchema) { this.eventSchema = eventSchema; return this; }
        public Builder webhookConfig(String webhookConfig) { this.webhookConfig = webhookConfig; return this; }
        public Builder branding(String branding) { this.branding = branding; return this; }
        public Builder programmeConfig(String programmeConfig) { this.programmeConfig = programmeConfig; return this; }
        public Builder programmeConfigVersion(Integer programmeConfigVersion) { this.programmeConfigVersion = programmeConfigVersion; return this; }
        public Builder dataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; return this; }
        public Builder maxActiveRules(Integer maxActiveRules) { this.maxActiveRules = maxActiveRules; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder version(Integer version) { this.version = version; return this; }

        public TenantConfig build() {
            return new TenantConfig(
                id,
                tenantId,
                displayName,
                subscriptionTier,
                status,
                ingestionModes,
                pointsCurrencyRate,
                dailyPointsCap,
                featureFlags,
                eventSchema,
                webhookConfig,
                branding,
                programmeConfig,
                programmeConfigVersion,
                dataResidencyRegion,
                maxActiveRules,
                createdAt,
                updatedAt,
                version
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public IdentityMode getIngestionModes() { return ingestionModes; }
    public void setIngestionModes(IdentityMode ingestionModes) { this.ingestionModes = ingestionModes; }
    public java.math.BigDecimal getPointsCurrencyRate() { return pointsCurrencyRate; }
    public void setPointsCurrencyRate(java.math.BigDecimal pointsCurrencyRate) { this.pointsCurrencyRate = pointsCurrencyRate; }
    public java.math.BigDecimal getDailyPointsCap() { return dailyPointsCap; }
    public void setDailyPointsCap(java.math.BigDecimal dailyPointsCap) { this.dailyPointsCap = dailyPointsCap; }
    public String getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(String featureFlags) { this.featureFlags = featureFlags; }
    public String getEventSchema() { return eventSchema; }
    public void setEventSchema(String eventSchema) { this.eventSchema = eventSchema; }
    public String getWebhookConfig() { return webhookConfig; }
    public void setWebhookConfig(String webhookConfig) { this.webhookConfig = webhookConfig; }
    public String getBranding() { return branding; }
    public void setBranding(String branding) { this.branding = branding; }
    public String getProgrammeConfig() { return programmeConfig; }
    public void setProgrammeConfig(String programmeConfig) { this.programmeConfig = programmeConfig; }
    public Integer getProgrammeConfigVersion() { return programmeConfigVersion; }
    public void setProgrammeConfigVersion(Integer programmeConfigVersion) { this.programmeConfigVersion = programmeConfigVersion; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public void setDataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; }
    public Integer getMaxActiveRules() { return maxActiveRules; }
    public void setMaxActiveRules(Integer maxActiveRules) { this.maxActiveRules = maxActiveRules; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}

