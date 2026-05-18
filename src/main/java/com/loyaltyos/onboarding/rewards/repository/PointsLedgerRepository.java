package com.loyaltyos.onboarding.rewards.repository;

import com.loyaltyos.onboarding.rules.entity.PointsLedger;
import com.loyaltyos.onboarding.rules.enums.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Ledger access for the reward engine. Native sums treat {@code CREDIT} as positive inflows and
 * {@code DEBIT}, {@code EXPIRE}, {@code REVERSAL} as outflows of the stored {@code points} magnitude.
 * {@code ADJUST} is summed as signed {@code points}; use only with explicit sign discipline.
 */
public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {

    List<PointsLedger> findByTenantIdAndCustomerIdAndIdempotencyKeyIn(
        String tenantId,
        String customerId,
        Collection<String> idempotencyKeys
    );

    boolean existsByTenantIdAndCustomerIdAndIdempotencyKey(String tenantId, String customerId, String idempotencyKey);

    Optional<PointsLedger> findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
        String tenantId,
        String customerId,
        String idempotencyKey
    );

    boolean existsByTenantIdAndCustomerIdAndEntryTypeAndReversalOfLedgerId(
        String tenantId,
        String customerId,
        LedgerEntryType entryType,
        Long reversalOfLedgerId
    );

    @Query(
        value = """
            SELECT COALESCE(SUM(
                CASE entry_type
                    WHEN 'CREDIT' THEN points
                    WHEN 'DEBIT' THEN -points
                    WHEN 'EXPIRE' THEN -points
                    WHEN 'REVERSAL' THEN -points
                    WHEN 'ADJUST' THEN points
                    ELSE 0
                END
            ), 0)
            FROM points_ledger
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND customer_id = :customerId
            """,
        nativeQuery = true
    )
    BigDecimal sumSignedPointsForCustomer(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("customerId") String customerId
    );

    @Query(
        value = """
            SELECT pl.id FROM points_ledger pl
            WHERE pl.entry_type = 'CREDIT'
              AND pl.expires_at IS NOT NULL
              AND pl.expires_at < :now
              AND NOT EXISTS (
                  SELECT 1 FROM points_ledger x
                  WHERE x.tenant_id = pl.tenant_id
                    AND x.customer_id = pl.customer_id
                    AND x.idempotency_key = CONCAT('exp:', pl.id)
              )
              AND NOT EXISTS (
                  SELECT 1 FROM points_ledger r
                  WHERE r.tenant_id = pl.tenant_id
                    AND r.customer_id = pl.customer_id
                    AND r.entry_type = 'REVERSAL'
                    AND r.reversal_of_ledger_id = pl.id
              )
            ORDER BY pl.id
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<Long> findGlobalCreditIdsEligibleForExpiry(@Param("now") Instant now, @Param("limit") int limit);

    @Query(
        value = """
            SELECT COALESCE(SUM(pl.points), 0)
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND pl.programme_uid = :programmeUid
              AND pl.customer_id = :customerId
              AND pl.entry_type = 'CREDIT'
              AND pl.expires_at IS NOT NULL
              AND pl.expires_at > :fromExclusive
              AND pl.expires_at <= :untilInclusive
              AND NOT EXISTS (
                  SELECT 1 FROM points_ledger x
                  WHERE x.tenant_id = pl.tenant_id
                    AND x.customer_id = pl.customer_id
                    AND x.idempotency_key = CONCAT('exp:', pl.id)
              )
              AND NOT EXISTS (
                  SELECT 1 FROM points_ledger r
                  WHERE r.tenant_id = pl.tenant_id
                    AND r.customer_id = pl.customer_id
                    AND r.entry_type = 'REVERSAL'
                    AND r.reversal_of_ledger_id = pl.id
              )
            """,
        nativeQuery = true
    )
    BigDecimal sumCreditPointsExpiringBetween(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("customerId") String customerId,
        @Param("fromExclusive") Instant fromExclusive,
        @Param("untilInclusive") Instant untilInclusive
    );
}
