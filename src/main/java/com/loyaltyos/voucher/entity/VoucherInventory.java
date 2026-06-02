package com.loyaltyos.voucher.entity;

import com.loyaltyos.voucher.enums.VoucherStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "voucher_inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_uid", columnNames = "inventory_uid"),
        @UniqueConstraint(name = "uk_tenant_code_hash", columnNames = {"tenant_id", "code_hash"}),
        @UniqueConstraint(name = "uk_redemption_id", columnNames = "redemption_id")
    },
    indexes = {
        @Index(name = "idx_issue", columnList = "tenant_id,programme_uid,catalog_reward_uid,status,created_at"),
        @Index(name = "idx_batch", columnList = "batch_uid"),
        @Index(name = "idx_customer", columnList = "tenant_id,customer_id,status"),
        @Index(name = "idx_status_expires", columnList = "status,expires_at")
    }
)
public class VoucherInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_uid", nullable = false, length = 128)
    private String inventoryUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "catalog_reward_uid", nullable = false, length = 64)
    private String catalogRewardUid;

    @Column(name = "denomination_mapping_id")
    private Long denominationMappingId;

    @Column(name = "batch_uid", nullable = false, length = 128)
    private String batchUid;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "code_ciphertext", nullable = false, length = 1024)
    private String codeCiphertext;

    @Column(name = "pin_hash", length = 64)
    private String pinHash;

    @Column(name = "pin_ciphertext", length = 512)
    private String pinCiphertext;

    @Column(name = "partner_sku", length = 128)
    private String partnerSku;

    @Column(name = "face_value", precision = 18, scale = 4)
    private BigDecimal faceValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VoucherStatus status = VoucherStatus.AVAILABLE;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Column(name = "customer_id", length = 128)
    private String customerId;

    @Column(name = "redemption_id", length = 128)
    private String redemptionId;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "partner_confirmation_ref", length = 255)
    private String partnerConfirmationRef;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_metadata_json", columnDefinition = "JSON")
    private String extraMetadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VoucherInventory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInventoryUid() { return inventoryUid; }
    public void setInventoryUid(String inventoryUid) { this.inventoryUid = inventoryUid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public Long getDenominationMappingId() { return denominationMappingId; }
    public void setDenominationMappingId(Long denominationMappingId) { this.denominationMappingId = denominationMappingId; }
    public String getBatchUid() { return batchUid; }
    public void setBatchUid(String batchUid) { this.batchUid = batchUid; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public String getCodeCiphertext() { return codeCiphertext; }
    public void setCodeCiphertext(String codeCiphertext) { this.codeCiphertext = codeCiphertext; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public String getPinCiphertext() { return pinCiphertext; }
    public void setPinCiphertext(String pinCiphertext) { this.pinCiphertext = pinCiphertext; }
    public String getPartnerSku() { return partnerSku; }
    public void setPartnerSku(String partnerSku) { this.partnerSku = partnerSku; }
    public BigDecimal getFaceValue() { return faceValue; }
    public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public VoucherStatus getStatus() { return status; }
    public void setStatus(VoucherStatus status) { this.status = status; }
    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public String getPartnerConfirmationRef() { return partnerConfirmationRef; }
    public void setPartnerConfirmationRef(String partnerConfirmationRef) {
        this.partnerConfirmationRef = partnerConfirmationRef;
    }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
    public String getExtraMetadataJson() { return extraMetadataJson; }
    public void setExtraMetadataJson(String extraMetadataJson) { this.extraMetadataJson = extraMetadataJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
