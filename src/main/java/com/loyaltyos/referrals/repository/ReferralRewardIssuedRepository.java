package com.loyaltyos.referrals.repository;

import com.loyaltyos.referrals.entity.ReferralRewardIssued;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferralRewardIssuedRepository extends JpaRepository<ReferralRewardIssued, Long> {
    Optional<ReferralRewardIssued> findByTenantIdAndRecipientCustomerIdAndIdempotencyKey(
        String tenantId, String recipientCustomerId, String idempotencyKey
    );

    List<ReferralRewardIssued> findByTenantIdAndProgrammeUid(String tenantId, String programmeUid);

    long countByTenantIdAndProgrammeUidAndRecipientCustomerIdAndStageAndRecipientType(
        String tenantId,
        String programmeUid,
        String recipientCustomerId,
        int stage,
        ReferralRewardIssued.RecipientType recipientType
    );

    @Query(
        "SELECT COALESCE(SUM(r.pointsAwarded), 0) FROM ReferralRewardIssued r "
            + "WHERE r.tenantId = :tenantId AND r.programmeUid = :programmeUid AND r.createdAt >= :since"
    )
    BigDecimal sumPointsAwardedSince(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("since") Instant since
    );
}

