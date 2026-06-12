package com.loyaltyos.merchants.repository;

import com.loyaltyos.merchants.entity.MerchantOnboardingAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantOnboardingAuditRepository extends JpaRepository<MerchantOnboardingAudit, Long> {

    List<MerchantOnboardingAudit> findByTenantIdAndMerchantUidOrderByCreatedAtDesc(
        String tenantId,
        String merchantUid
    );
}
