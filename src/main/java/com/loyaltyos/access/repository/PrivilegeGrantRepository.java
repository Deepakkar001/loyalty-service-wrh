package com.loyaltyos.access.repository;

import com.loyaltyos.access.entity.PrivilegeGrant;
import com.loyaltyos.access.enums.GrantEffect;
import com.loyaltyos.access.enums.GrantSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrivilegeGrantRepository extends JpaRepository<PrivilegeGrant, Long> {

    List<PrivilegeGrant> findByTenantIdAndSubjectTypeAndSubjectIdIn(
        String tenantId, GrantSubjectType subjectType, Collection<String> subjectIds);

    List<PrivilegeGrant> findByTenantIdAndSubjectTypeAndSubjectId(
        String tenantId, GrantSubjectType subjectType, String subjectId);

    void deleteByTenantIdAndSubjectTypeAndSubjectId(
        String tenantId, GrantSubjectType subjectType, String subjectId);

    long countByTenantIdAndSubjectType(String tenantId, GrantSubjectType subjectType);
}
