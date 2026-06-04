package com.loyaltyos.referrals.repository;

import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.enums.ReferralStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByTenantIdAndProgrammeUidAndReferralUid(String tenantId, String programmeUid, String referralUid);

    Optional<Referral> findByTenantIdAndProgrammeUidAndRefereeCustomerId(String tenantId, String programmeUid, String refereeCustomerId);

    List<Referral> findByTenantIdAndProgrammeUidAndReferrerCustomerId(String tenantId, String programmeUid, String referrerCustomerId);

    List<Referral> findByTenantIdAndProgrammeUid(String tenantId, String programmeUid);

    long countByTenantIdAndProgrammeUidAndReferrerCustomerIdAndStatusIn(
        String tenantId, String programmeUid, String referrerCustomerId, List<ReferralStatus> statuses
    );

    long countByTenantIdAndProgrammeUidAndReferrerCustomerIdAndStatusInAndCreatedAtAfter(
        String tenantId,
        String programmeUid,
        String referrerCustomerId,
        List<ReferralStatus> statuses,
        Instant createdAtAfter
    );

    List<Referral> findByTenantIdAndProgrammeUidAndStatusOrderByCreatedAtDesc(
        String tenantId, String programmeUid, ReferralStatus status
    );
}

