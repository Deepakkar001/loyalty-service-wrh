package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.entity.Programme;
import com.loyaltyos.onboarding.entity.Programme.ProgrammeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Long> {
    List<Programme> findByTenantIdOrderByCreatedAtAsc(String tenantId);
    List<Programme> findByTenantIdAndStatusNotOrderByCreatedAtAsc(String tenantId, ProgrammeStatus status);
    long countByTenantIdAndStatusNot(String tenantId, ProgrammeStatus status);
    Optional<Programme> findByTenantIdAndProgrammeUid(String tenantId, String programmeUid);
    boolean existsByTenantIdAndProgrammeUid(String tenantId, String programmeUid);
}

