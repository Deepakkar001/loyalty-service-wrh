package com.loyaltyos.onboarding.rewards.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link CustomerBalanceCache} (matches Flyway V26).
 */
public class CustomerBalanceCacheId implements Serializable {

    private String tenantId;
    private String programmeUid;
    private String customerId;

    public CustomerBalanceCacheId() {}

    public CustomerBalanceCacheId(String tenantId, String programmeUid, String customerId) {
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.customerId = customerId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerBalanceCacheId that = (CustomerBalanceCacheId) o;
        return Objects.equals(tenantId, that.tenantId)
            && Objects.equals(programmeUid, that.programmeUid)
            && Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, programmeUid, customerId);
    }
}
