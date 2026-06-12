package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.enums.TenantUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantUserRepository extends JpaRepository<TenantUser, String> {

    Optional<TenantUser> findByEmailIgnoreCase(String email);

    Optional<TenantUser> findByTenantIdAndEmailIgnoreCase(String tenantId, String email);

    List<TenantUser> findByTenantIdOrderByEmailAsc(String tenantId);

    boolean existsByTenantId(String tenantId);

    List<TenantUser> findByTenantId(String tenantId);
}
