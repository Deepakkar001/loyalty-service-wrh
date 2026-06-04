package com.loyaltyos.referrals.repository;

import com.loyaltyos.referrals.entity.ReferralProgramme;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralProgrammeRepository extends JpaRepository<ReferralProgramme, Long> {
    Optional<ReferralProgramme> findByTenantIdAndProgrammeUid(String tenantId, String programmeUid);
}

