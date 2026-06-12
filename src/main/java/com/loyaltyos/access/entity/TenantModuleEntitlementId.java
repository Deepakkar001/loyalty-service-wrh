package com.loyaltyos.access.entity;

import java.io.Serializable;
import java.util.Objects;

public class TenantModuleEntitlementId implements Serializable {

    private String tenantId;
    private String moduleKey;

    public TenantModuleEntitlementId() {}

    public TenantModuleEntitlementId(String tenantId, String moduleKey) {
        this.tenantId = tenantId;
        this.moduleKey = moduleKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantModuleEntitlementId that)) return false;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(moduleKey, that.moduleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, moduleKey);
    }
}
