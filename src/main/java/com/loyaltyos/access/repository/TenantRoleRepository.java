package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRoleRepository extends JpaRepository<TenantRole, String> {

    List<TenantRole> findByTenantIdOrderByRoleNameAsc(String tenantId);

    Optional<TenantRole> findByTenantIdAndSystemTrue(String tenantId);

    Optional<TenantRole> findByTenantIdAndRoleName(String tenantId, String roleName);

    boolean existsByTenantIdAndRoleName(String tenantId, String roleName);
}
