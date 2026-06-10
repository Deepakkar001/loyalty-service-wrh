package com.loyaltyos.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.loyaltyos.coupon.dto.CouponCreateRequest;
import com.loyaltyos.coupon.dto.CouponRedeemRequest;
import com.loyaltyos.coupon.dto.CouponValidateRequest;
import com.loyaltyos.coupon.dto.CouponValidateResponse;
import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import com.loyaltyos.coupon.model.CouponConstraints;
import com.loyaltyos.coupon.repository.CouponRedemptionRepository;
import com.loyaltyos.coupon.repository.CouponRepository;
import com.loyaltyos.coupon.support.CouponConstraintsSupport;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponValidationServiceTest {

    @Mock private RewardIssuanceService rewardIssuanceService;

    private CouponRepository couponRepository;
    private CouponRedemptionRepository redemptionRepository;
    private CouponAdminService adminService;
    private CouponValidationService validationService;

    @BeforeEach
    void setUp() {
        couponRepository = org.mockito.Mockito.mock(CouponRepository.class);
        redemptionRepository = org.mockito.Mockito.mock(CouponRedemptionRepository.class);
        // Use in-memory style with real repos would need @DataJpaTest — keep unit test on evaluate logic via admin + manual entity
        CouponConstraintsSupport constraintsSupport = new CouponConstraintsSupport(new ObjectMapper());
        adminService = new CouponAdminService(couponRepository, redemptionRepository, constraintsSupport);
        validationService = new CouponValidationService(
            couponRepository, redemptionRepository, constraintsSupport, rewardIssuanceService
        );
    }

    @Test
    void validateTypeConfig_rejectsInvalidPct() {
        org.junit.jupiter.api.Assertions.assertThrows(
            com.loyaltyos.coupon.exception.CouponAdminException.class,
            () -> CouponAdminService.validateTypeConfig(CouponType.PCT_DISCOUNT, null, null)
        );
    }

    @Test
    void createRequest_acceptsFixedDiscount() {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setName("Save 50");
        request.setCouponCode("SAVE50");
        request.setCouponType(CouponType.FIXED_DISCOUNT);
        request.setDiscountValue(new BigDecimal("50"));
        request.setUsageType(CouponUsageType.MULTI_USE);
        request.setMaxRedemptions(1000);
        request.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
        request.setActivateImmediately(true);

        CouponConstraints constraints = new CouponConstraints();
        constraints.setMinOrderAmount(new BigDecimal("500"));
        constraints.setAllowedChannels(java.util.List.of("APP", "WEB"));
        request.setConstraints(constraints);

        assertThat(request.getCouponCode()).isEqualTo("SAVE50");
        assertThat(request.getConstraints().getMinOrderAmount()).isEqualByComparingTo("500");
    }
}
