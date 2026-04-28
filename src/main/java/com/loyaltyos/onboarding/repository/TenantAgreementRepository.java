package com.loyaltyos.onboarding.repository;

import com.loyaltyos.onboarding.domain.entity.TenantAgreement;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantAgreementRepository extends JpaRepository<TenantAgreement, Long> {
    List<TenantAgreement> findByTenantId(String tenantId);
    List<TenantAgreement> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<TenantAgreement> findTopByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<TenantAgreement> findByStatus(AgreementStatus status);
    List<TenantAgreement> findByStatusNot(AgreementStatus status);
    Optional<TenantAgreement> findByAgreementUid(String agreementUid);
    long countByStatus(AgreementStatus status);
    void deleteByTenantId(String tenantId);
}

