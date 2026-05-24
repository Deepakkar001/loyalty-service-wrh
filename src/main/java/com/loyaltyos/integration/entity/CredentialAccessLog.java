package com.loyaltyos.integration.entity;

import com.loyaltyos.integration.enums.CredentialAccessType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "credential_access_log")
public class CredentialAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "api_key_uid", nullable = false, length = 128)
    private String apiKeyUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 32)
    private CredentialAccessType accessType;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CredentialAccessLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getApiKeyUid() { return apiKeyUid; }
    public void setApiKeyUid(String apiKeyUid) { this.apiKeyUid = apiKeyUid; }
    public CredentialAccessType getAccessType() { return accessType; }
    public void setAccessType(CredentialAccessType accessType) { this.accessType = accessType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
