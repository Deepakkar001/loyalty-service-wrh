package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.TenantRoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRoleTemplateRepository extends JpaRepository<TenantRoleTemplate, String> {
}
