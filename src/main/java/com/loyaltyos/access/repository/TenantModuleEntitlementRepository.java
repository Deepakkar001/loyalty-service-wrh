package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.entity.TenantModuleEntitlementId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantModuleEntitlementRepository extends JpaRepository<TenantModuleEntitlement, TenantModuleEntitlementId> {

    List<TenantModuleEntitlement> findByTenantIdAndEnabledTrue(String tenantId);

    List<TenantModuleEntitlement> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);

    long countByTenantId(String tenantId);
}
