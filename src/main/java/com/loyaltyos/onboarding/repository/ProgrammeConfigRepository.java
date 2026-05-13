package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.ProgrammeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgrammeConfigRepository extends JpaRepository<ProgrammeConfig, Long> {
    Optional<ProgrammeConfig> findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc(String tenantId, String programmeUid);
}

