package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.TierThresholdType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tier_definitions")
public class TierDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", length = 64)
    private String programmeUid;

    @Column(name = "tier_uid", nullable = false, length = 128)
    private String tierUid;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "entry_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal entryThreshold;

    @Column(name = "maintenance_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal maintenanceThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_type", nullable = false, length = 50)
    private TierThresholdType thresholdType;

    @Column(name = "points_multiplier", nullable = false, precision = 6, scale = 3)
    private BigDecimal pointsMultiplier = new BigDecimal("1.000");

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 90;

    @Column(name = "downgrade_warning_days", nullable = false)
    private Integer downgradeWarningDays = 60;

    @Column(name = "benefits", columnDefinition = "JSON")
    private String benefits;

    @Column(name = "is_invite_only")
    private Boolean inviteOnly = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public TierDefinition() {}

    public TierDefinition(
        Long id,
        String tenantId,
        String programmeUid,
        String tierUid,
        String name,
        Integer rankOrder,
        BigDecimal entryThreshold,
        BigDecimal maintenanceThreshold,
        TierThresholdType thresholdType,
        BigDecimal pointsMultiplier,
        Integer gracePeriodDays,
        Integer downgradeWarningDays,
        String benefits,
        Boolean inviteOnly,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.tierUid = tierUid;
        this.name = name;
        this.rankOrder = rankOrder;
        this.entryThreshold = entryThreshold;
        this.maintenanceThreshold = maintenanceThreshold;
        this.thresholdType = thresholdType;
        this.pointsMultiplier = pointsMultiplier != null ? pointsMultiplier : new BigDecimal("1.000");
        this.gracePeriodDays = gracePeriodDays != null ? gracePeriodDays : 90;
        this.downgradeWarningDays = downgradeWarningDays != null ? downgradeWarningDays : 60;
        this.benefits = benefits;
        this.inviteOnly = inviteOnly != null ? inviteOnly : false;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid;
        private String tierUid;
        private String name;
        private Integer rankOrder;
        private BigDecimal entryThreshold;
        private BigDecimal maintenanceThreshold;
        private TierThresholdType thresholdType;
        private BigDecimal pointsMultiplier = new BigDecimal("1.000");
        private Integer gracePeriodDays = 90;
        private Integer downgradeWarningDays = 60;
        private String benefits;
        private Boolean inviteOnly = false;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder tierUid(String tierUid) { this.tierUid = tierUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder rankOrder(Integer rankOrder) { this.rankOrder = rankOrder; return this; }
        public Builder entryThreshold(BigDecimal entryThreshold) { this.entryThreshold = entryThreshold; return this; }
        public Builder maintenanceThreshold(BigDecimal maintenanceThreshold) { this.maintenanceThreshold = maintenanceThreshold; return this; }
        public Builder thresholdType(TierThresholdType thresholdType) { this.thresholdType = thresholdType; return this; }
        public Builder pointsMultiplier(BigDecimal pointsMultiplier) { this.pointsMultiplier = pointsMultiplier; return this; }
        public Builder gracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; return this; }
        public Builder downgradeWarningDays(Integer downgradeWarningDays) { this.downgradeWarningDays = downgradeWarningDays; return this; }
        public Builder benefits(String benefits) { this.benefits = benefits; return this; }
        public Builder inviteOnly(Boolean inviteOnly) { this.inviteOnly = inviteOnly; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TierDefinition build() {
            return new TierDefinition(
                id,
                tenantId,
                programmeUid,
                tierUid,
                name,
                rankOrder,
                entryThreshold,
                maintenanceThreshold,
                thresholdType,
                pointsMultiplier,
                gracePeriodDays,
                downgradeWarningDays,
                benefits,
                inviteOnly,
                createdAt
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getTierUid() { return tierUid; }
    public void setTierUid(String tierUid) { this.tierUid = tierUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }
    public BigDecimal getEntryThreshold() { return entryThreshold; }
    public void setEntryThreshold(BigDecimal entryThreshold) { this.entryThreshold = entryThreshold; }
    public BigDecimal getMaintenanceThreshold() { return maintenanceThreshold; }
    public void setMaintenanceThreshold(BigDecimal maintenanceThreshold) { this.maintenanceThreshold = maintenanceThreshold; }
    public TierThresholdType getThresholdType() { return thresholdType; }
    public void setThresholdType(TierThresholdType thresholdType) { this.thresholdType = thresholdType; }
    public BigDecimal getPointsMultiplier() { return pointsMultiplier; }
    public void setPointsMultiplier(BigDecimal pointsMultiplier) { this.pointsMultiplier = pointsMultiplier; }
    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public Integer getDowngradeWarningDays() { return downgradeWarningDays; }
    public void setDowngradeWarningDays(Integer downgradeWarningDays) { this.downgradeWarningDays = downgradeWarningDays; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
    public Boolean getInviteOnly() { return inviteOnly; }
    public void setInviteOnly(Boolean inviteOnly) { this.inviteOnly = inviteOnly; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

