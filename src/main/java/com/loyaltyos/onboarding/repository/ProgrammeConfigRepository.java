package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeConfigRepository extends JpaRepository<ProgrammeConfig, Long> {
    Optional<ProgrammeConfig> findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc(String tenantId, String programmeUid);

    List<ProgrammeConfig> findByTenantIdAndProgrammeUidOrderByConfigVersionDesc(String tenantId, String programmeUid);

    List<ProgrammeConfig> findByTenantIdAndProgrammeUidOrderByConfigVersionAsc(String tenantId, String programmeUid);

    Optional<ProgrammeConfig> findByTenantIdAndProgrammeUidAndConfigVersion(
        String tenantId,
        String programmeUid,
        Integer configVersion
    );
}

