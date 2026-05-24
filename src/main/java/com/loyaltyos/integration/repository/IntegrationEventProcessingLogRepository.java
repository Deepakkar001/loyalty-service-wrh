package com.loyaltyos.integration.repository;

import com.loyaltyos.integration.entity.IntegrationEventProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loyaltyos.integration.enums.EventProcessingStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface IntegrationEventProcessingLogRepository extends JpaRepository<IntegrationEventProcessingLog, Long> {

    Optional<IntegrationEventProcessingLog> findByTenantIdAndEventId(String tenantId, String eventId);

    Optional<IntegrationEventProcessingLog> findByTenantIdAndEventIdAndCreatedAtAfter(
        String tenantId, String eventId, Instant since
    );

    @Query("""
        SELECT COUNT(e) FROM IntegrationEventProcessingLog e
        WHERE e.tenantId = :tenantId AND e.createdAt >= :since AND e.createdAt < :until
        AND e.processingStatus = :status
        """)
    long countByTenantAndStatusSince(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since,
        @Param("until") Instant until,
        @Param("status") EventProcessingStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(e.totalPointsAwarded), 0) FROM IntegrationEventProcessingLog e
        WHERE e.tenantId = :tenantId AND e.createdAt >= :since AND e.createdAt < :until
        AND e.processingStatus = :status
        """)
    BigDecimal sumPointsAwardedSince(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since,
        @Param("until") Instant until,
        @Param("status") EventProcessingStatus status
    );

    @Query("""
        SELECT COUNT(DISTINCT e.customerId) FROM IntegrationEventProcessingLog e
        WHERE e.tenantId = :tenantId AND e.createdAt >= :since AND e.createdAt < :until
        """)
    long countDistinctCustomersSince(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since,
        @Param("until") Instant until
    );
}
