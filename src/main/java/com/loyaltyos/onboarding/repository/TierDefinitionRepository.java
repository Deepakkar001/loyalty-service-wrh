package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TierDefinitionRepository extends JpaRepository<TierDefinition, Long> {
    List<TierDefinition> findByTenantIdOrderByRankOrderAsc(String tenantId);
    List<TierDefinition> findByTenantIdAndProgrammeUidOrderByRankOrderAsc(String tenantId, String programmeUid);
    long countByTenantId(String tenantId);
    void deleteByTenantId(String tenantId);
    void deleteByTenantIdAndProgrammeUid(String tenantId, String programmeUid);
}

