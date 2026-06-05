package com.loyaltyos.support.repository;

import com.loyaltyos.support.entity.SupportCase;
import com.loyaltyos.support.enums.SupportCaseStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {

    List<SupportCase> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<SupportCase> findByTenantIdAndCaseUid(String tenantId, String caseUid);

    Optional<SupportCase> findByCaseUid(String caseUid);

    List<SupportCase> findAllByOrderByCreatedAtDesc();

    List<SupportCase> findByStatusOrderByCreatedAtDesc(SupportCaseStatus status);

    long countByTenantIdAndCreatedAtAfter(String tenantId, Instant since);

    long countByStatus(SupportCaseStatus status);
}
