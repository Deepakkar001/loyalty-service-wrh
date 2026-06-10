package com.loyaltyos.coupon.scheduler;

import com.loyaltyos.coupon.config.CouponProperties;
import com.loyaltyos.coupon.enums.CouponStatus;
import com.loyaltyos.coupon.repository.CouponRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "loyaltyos.coupon.enabled", havingValue = "true", matchIfMissing = true)
public class CouponExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(CouponExpiryScheduler.class);

    private final CouponProperties properties;
    private final CouponRepository couponRepository;

    public CouponExpiryScheduler(CouponProperties properties, CouponRepository couponRepository) {
        this.properties = properties;
        this.couponRepository = couponRepository;
    }

    @Scheduled(cron = "${loyaltyos.coupon.expiry-cron:0 15 2 * * *}")
    @ConditionalOnProperty(name = "loyaltyos.coupon.expiry-job-enabled", havingValue = "true", matchIfMissing = true)
    @Transactional
    public void markExpiredCoupons() {
        if (!properties.isEnabled()) {
            return;
        }
        int updated = couponRepository.markExpired(CouponStatus.ACTIVE, CouponStatus.EXPIRED, Instant.now());
        if (updated > 0) {
            log.info("Marked {} coupons as EXPIRED", updated);
        }
    }
}
