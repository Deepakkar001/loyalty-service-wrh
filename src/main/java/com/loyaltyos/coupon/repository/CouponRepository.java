package com.loyaltyos.coupon.repository;

import com.loyaltyos.coupon.entity.Coupon;
import com.loyaltyos.coupon.enums.CouponStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByTenantIdAndCouponUid(String tenantId, String couponUid);

    Optional<Coupon> findByTenantIdAndCouponCode(String tenantId, String couponCode);

    List<Coupon> findByTenantIdAndProgrammeUidOrderByCreatedAtDesc(String tenantId, String programmeUid);

    List<Coupon> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @Query("""
        SELECT c FROM Coupon c
        WHERE c.status = :status AND c.validUntil < :now
        """)
    List<Coupon> findExpiredActive(@Param("status") CouponStatus status, @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE Coupon c SET c.status = :newStatus, c.updatedAt = :now
        WHERE c.status = :oldStatus AND c.validUntil < :now
        """)
    int markExpired(@Param("oldStatus") CouponStatus oldStatus,
                    @Param("newStatus") CouponStatus newStatus,
                    @Param("now") Instant now);
}
