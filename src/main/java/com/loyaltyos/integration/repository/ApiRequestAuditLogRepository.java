package com.loyaltyos.integration.repository;

import com.loyaltyos.integration.entity.ApiRequestAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ApiRequestAuditLogRepository extends JpaRepository<ApiRequestAuditLog, Long> {

    @Query(
        value = """
            SELECT a FROM ApiRequestAuditLog a
            WHERE a.tenantId = :tenantId
            ORDER BY a.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(a) FROM ApiRequestAuditLog a
            WHERE a.tenantId = :tenantId
            """
    )
    Page<ApiRequestAuditLog> findByTenantIdOrderByCreatedAtDesc(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(a) FROM ApiRequestAuditLog a
        WHERE a.tenantId = :tenantId AND a.createdAt >= :since
        """)
    long countByTenantIdSince(@Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query("""
        SELECT COUNT(a) FROM ApiRequestAuditLog a
        WHERE a.tenantId = :tenantId AND a.createdAt >= :since AND a.httpStatus >= 200 AND a.httpStatus < 300
        """)
    long countSuccessfulByTenantIdSince(@Param("tenantId") String tenantId, @Param("since") Instant since);

    List<ApiRequestAuditLog> findTop10ByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Query("""
        SELECT a.processingTimeMs FROM ApiRequestAuditLog a
        WHERE a.tenantId = :tenantId AND a.createdAt >= :since AND a.httpStatus >= 200 AND a.httpStatus < 300
        ORDER BY a.processingTimeMs
        """)
    List<Integer> findLatenciesSince(@Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query("""
        SELECT COUNT(a) FROM ApiRequestAuditLog a
        WHERE a.apiKeyUid = :apiKeyUid AND a.tenantId = :tenantId AND a.createdAt >= :since
        """)
    long countByApiKeyUidSince(
        @Param("apiKeyUid") String apiKeyUid,
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    @Query("""
        SELECT a.httpStatus, COUNT(a) FROM ApiRequestAuditLog a
        WHERE a.tenantId = :tenantId AND a.createdAt >= :since AND a.createdAt < :until
        GROUP BY a.httpStatus
        """)
    List<Object[]> countByHttpStatusGrouped(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since,
        @Param("until") Instant until
    );

    @Query("""
        SELECT MAX(a.createdAt) FROM ApiRequestAuditLog a WHERE a.tenantId = :tenantId
        """)
    Instant findLastRequestAt(@Param("tenantId") String tenantId);
}
