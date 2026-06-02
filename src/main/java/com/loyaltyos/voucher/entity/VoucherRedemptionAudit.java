package com.loyaltyos.voucher.entity;

import com.loyaltyos.voucher.enums.VoucherAuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "voucher_redemption_audit",
    indexes = {
        @Index(name = "idx_tenant_customer", columnList = "tenant_id,customer_id,created_at"),
        @Index(name = "idx_redemption_id", columnList = "redemption_id"),
        @Index(name = "idx_inventory_uid", columnList = "inventory_uid")
    }
)
public class VoucherRedemptionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_uid", nullable = false, unique = true, length = 128)
    private String auditUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "inventory_uid", length = 128)
    private String inventoryUid;

    @Column(name = "redemption_id", nullable = false, length = 128)
    private String redemptionId;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private VoucherAuditEventType eventType;

    @Column(name = "actor", length = 255)
    private String actor;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VoucherRedemptionAudit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAuditUid() { return auditUid; }
    public void setAuditUid(String auditUid) { this.auditUid = auditUid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getInventoryUid() { return inventoryUid; }
    public void setInventoryUid(String inventoryUid) { this.inventoryUid = inventoryUid; }
    public String getRedemptionId() { return redemptionId; }
    public void setRedemptionId(String redemptionId) { this.redemptionId = redemptionId; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public VoucherAuditEventType getEventType() { return eventType; }
    public void setEventType(VoucherAuditEventType eventType) { this.eventType = eventType; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
