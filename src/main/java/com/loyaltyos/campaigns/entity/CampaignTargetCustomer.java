package com.loyaltyos.campaigns.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "campaign_target_customers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_campaign_customer",
        columnNames = {"tenant_id", "campaign_uid", "customer_id"}
    ),
    indexes = @Index(name = "idx_tenant_campaign", columnList = "tenant_id,campaign_uid")
)
public class CampaignTargetCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "campaign_uid", nullable = false, length = 128)
    private String campaignUid;

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @Column(name = "added_by", length = 255)
    private String addedBy;

    @Column(name = "source_upload_uid", length = 128)
    private String sourceUploadUid;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCampaignUid() { return campaignUid; }
    public void setCampaignUid(String campaignUid) { this.campaignUid = campaignUid; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public String getSourceUploadUid() { return sourceUploadUid; }
    public void setSourceUploadUid(String sourceUploadUid) { this.sourceUploadUid = sourceUploadUid; }
}
