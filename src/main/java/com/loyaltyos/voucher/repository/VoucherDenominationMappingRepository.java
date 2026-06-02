package com.loyaltyos.voucher.repository;

import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherDenominationMappingRepository extends JpaRepository<VoucherDenominationMapping, Long> {

    List<VoucherDenominationMapping> findByTenantIdAndCatalogRewardUidAndActiveTrueOrderByPriorityAsc(
        String tenantId,
        String catalogRewardUid
    );

    boolean existsByTenantIdAndCatalogRewardUidAndActiveTrue(String tenantId, String catalogRewardUid);

    Optional<VoucherDenominationMapping> findByTenantIdAndCatalogRewardUidAndPointsRequiredAndActiveTrue(
        String tenantId,
        String catalogRewardUid,
        BigDecimal pointsRequired
    );

    Optional<VoucherDenominationMapping> findByTenantIdAndCatalogRewardUidAndFaceValueAndActiveTrue(
        String tenantId,
        String catalogRewardUid,
        BigDecimal faceValue
    );

    @Modifying
    @Query("""
        UPDATE VoucherDenominationMapping m
        SET m.active = false
        WHERE m.tenantId = :tenantId AND m.catalogRewardUid = :catalogRewardUid AND m.active = true
        """)
    int deactivateAllForCatalog(
        @Param("tenantId") String tenantId,
        @Param("catalogRewardUid") String catalogRewardUid
    );
}
