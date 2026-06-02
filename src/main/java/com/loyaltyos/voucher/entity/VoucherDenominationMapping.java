package com.loyaltyos.voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "voucher_denomination_mapping",
    indexes = {
        @Index(name = "idx_tenant_catalog_active", columnList = "tenant_id,catalog_reward_uid,is_active"),
        @Index(name = "idx_catalog_face", columnList = "catalog_reward_uid,face_value,is_active")
    }
)
public class VoucherDenominationMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mapping_uid", nullable = false, unique = true, length = 128)
    private String mappingUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "catalog_reward_uid", nullable = false, length = 64)
    private String catalogRewardUid;

    @Column(name = "points_required", nullable = false, precision = 18, scale = 4)
    private BigDecimal pointsRequired;

    @Column(name = "face_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal faceValue;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "partner_sku", length = 128)
    private String partnerSku;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMappingUid() { return mappingUid; }
    public void setMappingUid(String mappingUid) { this.mappingUid = mappingUid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public BigDecimal getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(BigDecimal pointsRequired) { this.pointsRequired = pointsRequired; }
    public BigDecimal getFaceValue() { return faceValue; }
    public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPartnerSku() { return partnerSku; }
    public void setPartnerSku(String partnerSku) { this.partnerSku = partnerSku; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
