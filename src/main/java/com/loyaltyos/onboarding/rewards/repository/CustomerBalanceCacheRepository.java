package com.loyaltyos.onboarding.rewards.repository;

import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCache;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCacheId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CustomerBalanceCacheRepository extends JpaRepository<CustomerBalanceCache, CustomerBalanceCacheId> {

    Slice<CustomerBalanceCache> findByTenantId(String tenantId, Pageable pageable);

    @Query(
        value = "SELECT DISTINCT tenant_id FROM customer_balance_cache ORDER BY tenant_id LIMIT :limit",
        nativeQuery = true
    )
    List<String> findDistinctTenantIdsLimited(@Param("limit") int limit);

    /**
     * Atomically increments balance for the composite PK row (insert-on-first-use).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            INSERT INTO customer_balance_cache (tenant_id, programme_uid, customer_id, balance, updated_at)
            VALUES (:tenantId, :programmeUid, :customerId, :delta, NOW(6))
            ON DUPLICATE KEY UPDATE
                balance = balance + VALUES(balance),
                updated_at = NOW(6)
            """,
        nativeQuery = true
    )
    void incrementBalance(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("customerId") String customerId,
        @Param("delta") BigDecimal delta
    );

    /**
     * Decrements balance only if current balance is sufficient (prevents negative cache).
     *
     * @return rows updated (1 if applied, 0 if insufficient or row missing)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            UPDATE customer_balance_cache
            SET balance = balance - :delta,
                updated_at = NOW(6)
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND customer_id = :customerId
              AND balance >= :delta
            """,
        nativeQuery = true
    )
    int decrementBalanceIfSufficient(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("customerId") String customerId,
        @Param("delta") BigDecimal delta
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            UPDATE customer_balance_cache
            SET balance = :balance,
                updated_at = NOW(6)
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND customer_id = :customerId
            """,
        nativeQuery = true
    )
    int setBalanceExact(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("customerId") String customerId,
        @Param("balance") BigDecimal balance
    );
}
