package com.loyaltyos.referrals.repository;

import com.loyaltyos.referrals.entity.ReferralCode;
import com.loyaltyos.referrals.enums.ReferralCodeStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {
    Optional<ReferralCode> findByTenantIdAndProgrammeUidAndCustomerIdAndStatus(
        String tenantId, String programmeUid, String customerId, ReferralCodeStatus status
    );

    Optional<ReferralCode> findByTenantIdAndCodeAndStatus(String tenantId, String code, ReferralCodeStatus status);

    boolean existsByTenantIdAndCode(String tenantId, String code);
}

