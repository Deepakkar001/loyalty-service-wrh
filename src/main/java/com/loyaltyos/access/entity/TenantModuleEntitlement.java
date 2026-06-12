package com.loyaltyos.access.entity;

import com.loyaltyos.access.enums.EntitlementSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenant_module_entitlement")
@IdClass(TenantModuleEntitlementId.class)
public class TenantModuleEntitlement {

    @Id
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Id
    @Column(name = "module_key", length = 64)
    private String moduleKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private EntitlementSource source;

    @Column(name = "enabled_by", length = 128)
    private String enabledBy;

    @CreationTimestamp
    @Column(name = "enabled_at", nullable = false, updatable = false)
    private Instant enabledAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public TenantModuleEntitlement() {}

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public EntitlementSource getSource() { return source; }
    public void setSource(EntitlementSource source) { this.source = source; }
    public String getEnabledBy() { return enabledBy; }
    public void setEnabledBy(String enabledBy) { this.enabledBy = enabledBy; }
    public Instant getEnabledAt() { return enabledAt; }
    public void setEnabledAt(Instant enabledAt) { this.enabledAt = enabledAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
