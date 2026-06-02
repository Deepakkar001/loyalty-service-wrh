package com.loyaltyos.voucher.repository;

import com.loyaltyos.voucher.entity.VoucherInventory;
import com.loyaltyos.voucher.enums.VoucherStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherInventoryRepository extends JpaRepository<VoucherInventory, Long> {

    Optional<VoucherInventory> findByRedemptionId(String redemptionId);

    boolean existsByTenantIdAndCodeHash(String tenantId, String codeHash);

    @Query("""
        SELECT COUNT(v) FROM VoucherInventory v
        WHERE v.tenantId = :tenantId
          AND v.programmeUid = :programmeUid
          AND v.catalogRewardUid = :catalogRewardUid
          AND v.status = com.loyaltyos.voucher.enums.VoucherStatus.AVAILABLE
          AND (v.expiresAt IS NULL OR v.expiresAt > CURRENT_TIMESTAMP)
        """)
    long countAvailable(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("catalogRewardUid") String catalogRewardUid
    );

    List<VoucherInventory> findByTenantIdAndCustomerIdAndStatusOrderByIssuedAtDesc(
        String tenantId,
        String customerId,
        VoucherStatus status
    );

    List<VoucherInventory> findByStatusAndExpiresAtBefore(VoucherStatus status, Instant expiresAt);

    @Query(
        value = """
            SELECT id FROM voucher_inventory
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND catalog_reward_uid = :catalogRewardUid
              AND status = 'AVAILABLE'
              AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(6))
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """,
        nativeQuery = true
    )
    Optional<Long> lockNextAvailableId(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("catalogRewardUid") String catalogRewardUid
    );

    @Query("""
        SELECT COUNT(v) FROM VoucherInventory v
        WHERE v.tenantId = :tenantId
          AND v.programmeUid = :programmeUid
          AND v.catalogRewardUid = :catalogRewardUid
          AND v.faceValue = :faceValue
          AND v.status = com.loyaltyos.voucher.enums.VoucherStatus.AVAILABLE
          AND (v.expiresAt IS NULL OR v.expiresAt > CURRENT_TIMESTAMP)
        """)
    long countAvailableByFaceValue(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("catalogRewardUid") String catalogRewardUid,
        @Param("faceValue") java.math.BigDecimal faceValue
    );

    @Query(
        value = """
            SELECT id FROM voucher_inventory
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND catalog_reward_uid = :catalogRewardUid
              AND face_value = :faceValue
              AND status = 'AVAILABLE'
              AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP(6))
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """,
        nativeQuery = true
    )
    Optional<Long> lockNextAvailableIdByFaceValue(
        @Param("tenantId") String tenantId,
        @Param("programmeUid") String programmeUid,
        @Param("catalogRewardUid") String catalogRewardUid,
        @Param("faceValue") java.math.BigDecimal faceValue
    );
}
