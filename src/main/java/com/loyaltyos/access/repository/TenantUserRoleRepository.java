package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.TenantUserRole;
import com.loyaltyos.access.entity.TenantUserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantUserRoleRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {

    List<TenantUserRole> findByUserId(String userId);

    List<TenantUserRole> findByRoleId(String roleId);

    void deleteByUserId(String userId);
}
