package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.TenantContact;
import com.loyaltyos.onboarding.domain.enums.ContactRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantContactRepository extends JpaRepository<TenantContact, Long> {
    List<TenantContact> findByTenantId(String tenantId);
    Optional<TenantContact> findByTenantIdAndRole(String tenantId, ContactRole role);
    void deleteByTenantId(String tenantId);
}

