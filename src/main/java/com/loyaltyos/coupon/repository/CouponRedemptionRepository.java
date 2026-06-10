package com.loyaltyos.coupon.repository;

import com.loyaltyos.coupon.entity.CouponRedemption;
import com.loyaltyos.coupon.enums.CouponRedemptionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    Optional<CouponRedemption> findByTenantIdAndOrderIdAndCouponCode(
        String tenantId, String orderId, String couponCode
    );

    long countByTenantIdAndCouponUidAndCustomerIdAndStatus(
        String tenantId, String couponUid, String customerId, CouponRedemptionStatus status
    );

    List<CouponRedemption> findByTenantIdAndCouponUidOrderByRedeemedAtDesc(
        String tenantId, String couponUid
    );
}
