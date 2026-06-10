package com.loyaltyos.coupon.service;

import com.loyaltyos.coupon.dto.CouponRedeemRequest;
import com.loyaltyos.coupon.dto.CouponRedeemResponse;
import com.loyaltyos.coupon.dto.CouponValidateRequest;
import com.loyaltyos.coupon.dto.CouponValidateResponse;
import com.loyaltyos.coupon.entity.Coupon;
import com.loyaltyos.coupon.entity.CouponRedemption;
import com.loyaltyos.coupon.enums.CouponRedemptionStatus;
import com.loyaltyos.coupon.enums.CouponStatus;
import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import com.loyaltyos.coupon.enums.CouponValidationReason;
import com.loyaltyos.coupon.model.CouponConstraints;
import com.loyaltyos.coupon.repository.CouponRedemptionRepository;
import com.loyaltyos.coupon.repository.CouponRepository;
import com.loyaltyos.coupon.support.CouponCodeNormalizer;
import com.loyaltyos.coupon.support.CouponConstraintsSupport;
import com.loyaltyos.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import com.loyaltyos.rewards.dto.RewardIssueRequest;
import com.loyaltyos.rewards.dto.RewardIssueResponse;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponValidationService {

    private static final int MONEY_SCALE = 2;

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final CouponConstraintsSupport constraintsSupport;
    private final RewardIssuanceService rewardIssuanceService;

    public CouponValidationService(
        CouponRepository couponRepository,
        CouponRedemptionRepository redemptionRepository,
        CouponConstraintsSupport constraintsSupport,
        RewardIssuanceService rewardIssuanceService
    ) {
        this.couponRepository = Objects.requireNonNull(couponRepository);
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository);
        this.constraintsSupport = Objects.requireNonNull(constraintsSupport);
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService);
    }

    @Transactional(readOnly = true)
    public CouponValidateResponse validate(
        String tenantId,
        String couponCode,
        CouponValidateRequest request
    ) {
        return evaluate(tenantId, couponCode, request, false);
    }

    @Transactional(readOnly = true)
    public CouponValidateResponse getDetails(
        String tenantId,
        String couponCode,
        String programmeUid
    ) {
        String normalized = CouponCodeNormalizer.normalize(couponCode);
        Coupon coupon = couponRepository.findByTenantIdAndCouponCode(tenantId, normalized).orElse(null);
        if (coupon == null) {
            return invalid(CouponValidationReason.NOT_FOUND, "Coupon not found");
        }
        if (programmeUid != null && !programmeUid.isBlank()
            && !normalizeProgramme(programmeUid).equals(coupon.getProgrammeUid())) {
            return invalid(CouponValidationReason.NOT_FOUND, "Coupon not found for programme");
        }
        CouponValidateResponse response = new CouponValidateResponse();
        response.setValid(coupon.getStatus() == CouponStatus.ACTIVE);
        response.setStatus(coupon.getStatus().name());
        response.setReason(coupon.getStatus() == CouponStatus.ACTIVE
            ? CouponValidationReason.VALID : CouponValidationReason.INACTIVE);
        response.setMessage(coupon.getStatus() == CouponStatus.ACTIVE
            ? "Coupon is active" : "Coupon is not active");
        populateCouponDetails(response, coupon, null);
        return response;
    }

    @Transactional
    public CouponRedeemResponse redeem(
        String tenantId,
        String couponCode,
        CouponRedeemRequest request
    ) {
        String normalized = CouponCodeNormalizer.normalize(couponCode);
        String orderId = request.getOrderId().trim();

        var existing = redemptionRepository.findByTenantIdAndOrderIdAndCouponCode(
            tenantId, orderId, normalized
        );
        if (existing.isPresent()) {
            return toReplayResponse(existing.get(), tenantId, request.getProgrammeUid(), request.getCustomerId());
        }

        CouponValidateRequest validateReq = new CouponValidateRequest();
        validateReq.setProgrammeUid(request.getProgrammeUid());
        validateReq.setCustomerId(request.getCustomerId());
        validateReq.setOrderAmount(request.getOrderAmount());
        validateReq.setChannel(request.getChannel());
        validateReq.setOtherCouponsApplied(request.getOtherCouponsApplied());

        CouponValidateResponse validation = evaluate(tenantId, couponCode, validateReq, true);
        if (!validation.isValid()) {
            CouponRedeemResponse failure = new CouponRedeemResponse();
            failure.setStatus("VALIDATION_FAILED");
            failure.setCouponCode(normalized);
            failure.setMessage(validation.getMessage());
            return failure;
        }

        Coupon coupon = couponRepository.findByTenantIdAndCouponCode(tenantId, normalized)
            .orElseThrow();

        Instant now = Instant.now();
        CouponRedemption redemption = new CouponRedemption();
        redemption.setRedemptionUid(UUID.randomUUID().toString());
        redemption.setTenantId(tenantId);
        redemption.setProgrammeUid(normalizeProgramme(request.getProgrammeUid()));
        redemption.setCouponUid(coupon.getCouponUid());
        redemption.setCouponCode(normalized);
        redemption.setCustomerId(request.getCustomerId().trim());
        redemption.setOrderId(orderId);
        redemption.setChannel(trimToNull(request.getChannel()));
        redemption.setOrderAmount(request.getOrderAmount());
        redemption.setDiscountAmount(validation.getDiscountAmount());
        redemption.setStatus(CouponRedemptionStatus.REDEEMED);
        redemption.setRedeemedAt(now);

        BigDecimal pointsCredited = null;
        Long ledgerId = null;
        BigDecimal newBalance = null;

        if (coupon.getCouponType() == CouponType.POINTS_BONUS && validation.getPointsToCredit() != null) {
            RewardIssueRequest issueRequest = new RewardIssueRequest();
            issueRequest.setProgrammeUid(redemption.getProgrammeUid());
            issueRequest.setCustomerId(redemption.getCustomerId());
            issueRequest.setEventId("coupon:" + orderId);
            issueRequest.setNarrative("Coupon bonus: " + normalized);

            RewardIssueCommandDto command = new RewardIssueCommandDto();
            command.setActionType("AWARD_POINTS");
            command.setPointsToAward(validation.getPointsToCredit());
            command.setIdempotencyKey("coupon:" + normalized + ":" + orderId);
            issueRequest.getRewardCommands().add(command);

            RewardIssueResponse issued = rewardIssuanceService.issue(tenantId, issueRequest);
            pointsCredited = validation.getPointsToCredit();
            if (issued.getLedgerLines() != null && !issued.getLedgerLines().isEmpty()) {
                ledgerId = issued.getLedgerLines().getFirst().getLedgerId();
            }
            newBalance = issued.getNewBalance();
            redemption.setPointsCredited(pointsCredited);
            redemption.setLedgerId(ledgerId);
        }

        coupon.setRedemptionCount(coupon.getRedemptionCount() + 1);
        if (coupon.getRedemptionCount() >= coupon.getMaxRedemptions()) {
            coupon.setStatus(CouponStatus.EXHAUSTED);
        }
        coupon.setUpdatedAt(now);
        couponRepository.save(coupon);
        redemptionRepository.save(redemption);

        CouponRedeemResponse response = new CouponRedeemResponse();
        response.setStatus("SUCCESS");
        response.setRedemptionUid(redemption.getRedemptionUid());
        response.setCouponUid(coupon.getCouponUid());
        response.setCouponCode(normalized);
        response.setCouponType(coupon.getCouponType());
        response.setDiscountAmount(validation.getDiscountAmount());
        response.setFinalAmount(validation.getFinalAmount());
        response.setPointsCredited(pointsCredited);
        response.setLedgerId(ledgerId);
        response.setRedeemedAt(now);
        response.setIdempotentReplay(false);
        if (newBalance == null && coupon.getCouponType() == CouponType.POINTS_BONUS) {
            RewardBalanceResponse balance = rewardIssuanceService.getBalance(
                tenantId, redemption.getProgrammeUid(), redemption.getCustomerId()
            );
            newBalance = balance.getBalance();
        }
        response.setNewBalance(newBalance);
        return response;
    }

    private CouponValidateResponse evaluate(
        String tenantId,
        String couponCode,
        CouponValidateRequest request,
        boolean forRedeem
    ) {
        String normalized = CouponCodeNormalizer.normalize(couponCode);
        if (normalized == null || normalized.isBlank()) {
            return invalid(CouponValidationReason.NOT_FOUND, "Coupon code is required");
        }

        Coupon coupon = couponRepository.findByTenantIdAndCouponCode(tenantId, normalized).orElse(null);
        if (coupon == null) {
            return invalid(CouponValidationReason.NOT_FOUND, "Coupon not found");
        }

        String programme = normalizeProgramme(request.getProgrammeUid());
        if (!programme.equals(coupon.getProgrammeUid())) {
            return invalid(CouponValidationReason.NOT_FOUND, "Coupon not found for programme");
        }

        Instant now = Instant.now();
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            return invalid(mapInactiveReason(coupon.getStatus()), "Coupon is " + coupon.getStatus().name().toLowerCase());
        }
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            return invalid(CouponValidationReason.NOT_YET_VALID, "Coupon is not yet valid");
        }
        if (coupon.getValidUntil().isBefore(now)) {
            return invalid(CouponValidationReason.EXPIRED, "Coupon has expired");
        }
        if (coupon.getRedemptionCount() >= coupon.getMaxRedemptions()) {
            return invalid(CouponValidationReason.EXHAUSTED, "Coupon redemption limit reached");
        }

        String customerId = request.getCustomerId() != null ? request.getCustomerId().trim() : null;
        if (customerId == null || customerId.isBlank()) {
            return invalid(CouponValidationReason.CUSTOMER_NOT_ELIGIBLE, "customerId is required");
        }

        if (coupon.getTargetCustomerId() != null
            && !coupon.getTargetCustomerId().equalsIgnoreCase(customerId)) {
            return invalid(CouponValidationReason.CUSTOMER_NOT_ELIGIBLE, "Coupon is not assigned to this customer");
        }

        long customerUses = redemptionRepository.countByTenantIdAndCouponUidAndCustomerIdAndStatus(
            tenantId, coupon.getCouponUid(), customerId, CouponRedemptionStatus.REDEEMED
        );
        if (customerUses >= coupon.getMaxRedemptionsPerCustomer()) {
            return invalid(CouponValidationReason.ALREADY_USED_BY_CUSTOMER, "Customer has already used this coupon");
        }
        if (coupon.getUsageType() == CouponUsageType.SINGLE_USE && coupon.getRedemptionCount() > 0 && forRedeem) {
            return invalid(CouponValidationReason.EXHAUSTED, "Single-use coupon already redeemed");
        }

        CouponConstraints constraints = constraintsSupport.parse(coupon.getConstraintsJson());
        if (request.getChannel() != null && !request.getChannel().isBlank()
            && constraints.getAllowedChannels() != null && !constraints.getAllowedChannels().isEmpty()) {
            boolean allowed = constraints.getAllowedChannels().stream()
                .anyMatch(ch -> ch.equalsIgnoreCase(request.getChannel().trim()));
            if (!allowed) {
                return invalid(CouponValidationReason.CHANNEL_NOT_ALLOWED, "Coupon not valid on this channel");
            }
        }

        if (!coupon.isStackable() && request.getOtherCouponsApplied() != null
            && !request.getOtherCouponsApplied().isEmpty()) {
            return invalid(CouponValidationReason.NON_STACKABLE_CONFLICT,
                "Non-stackable coupon cannot be combined with other coupons");
        }
        if (request.getOtherCouponsApplied() != null) {
            for (String other : request.getOtherCouponsApplied()) {
                if (other != null && other.equalsIgnoreCase(normalized)) {
                    continue;
                }
                Coupon otherCoupon = couponRepository.findByTenantIdAndCouponCode(
                    tenantId, CouponCodeNormalizer.normalize(other)
                ).orElse(null);
                if (otherCoupon != null && !otherCoupon.isStackable()) {
                    return invalid(CouponValidationReason.NON_STACKABLE_CONFLICT,
                        "Cannot combine with non-stackable coupon " + other);
                }
            }
        }

        BigDecimal orderAmount = request.getOrderAmount() != null ? request.getOrderAmount() : BigDecimal.ZERO;
        if (constraints.getMinOrderAmount() != null
            && orderAmount.compareTo(constraints.getMinOrderAmount()) < 0) {
            return invalid(CouponValidationReason.MIN_ORDER_NOT_MET,
                "Minimum order amount is " + constraints.getMinOrderAmount());
        }

        BenefitCalculation benefit = calculateBenefit(coupon, constraints, orderAmount);
        if (benefit.reason != null) {
            return invalid(benefit.reason, benefit.message);
        }

        CouponValidateResponse response = new CouponValidateResponse();
        response.setValid(true);
        response.setStatus("VALID");
        response.setReason(CouponValidationReason.VALID);
        response.setMessage("Coupon is valid");
        populateCouponDetails(response, coupon, constraints);
        response.setDiscountAmount(benefit.discountAmount);
        response.setFinalAmount(benefit.finalAmount);
        response.setPointsToCredit(benefit.pointsToCredit);
        return response;
    }

    private void populateCouponDetails(
        CouponValidateResponse response,
        Coupon coupon,
        CouponConstraints constraints
    ) {
        if (constraints == null) {
            constraints = constraintsSupport.parse(coupon.getConstraintsJson());
        }
        response.setCouponUid(coupon.getCouponUid());
        response.setCouponCode(coupon.getCouponCode());
        response.setCouponName(coupon.getName());
        response.setCouponType(coupon.getCouponType());
        response.setStackable(coupon.isStackable());
        response.setValidUntil(coupon.getValidUntil());
        response.setCurrency(constraints.getCurrency() != null ? constraints.getCurrency() : "INR");
        if (coupon.getCouponType() == CouponType.FREE_ITEM) {
            response.setFreeItemSku(constraints.getFreeItemSku());
            response.setFreeItemLabel(constraints.getFreeItemLabel());
        }
    }

    private BenefitCalculation calculateBenefit(
        Coupon coupon,
        CouponConstraints constraints,
        BigDecimal orderAmount
    ) {
        BigDecimal normalizedOrder = orderAmount.max(BigDecimal.ZERO);
        return switch (coupon.getCouponType()) {
            case FIXED_DISCOUNT -> {
                BigDecimal discount = scaleMoney(coupon.getDiscountValue());
                yield new BenefitCalculation(discount, normalizedOrder.subtract(discount).max(BigDecimal.ZERO), null, null, null);
            }
            case PCT_DISCOUNT -> {
                BigDecimal pct = coupon.getDiscountPct();
                BigDecimal discount = scaleMoney(normalizedOrder.multiply(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                if (constraints.getMaxDiscountCap() != null) {
                    discount = discount.min(constraints.getMaxDiscountCap());
                }
                discount = scaleMoney(discount);
                yield new BenefitCalculation(discount, normalizedOrder.subtract(discount).max(BigDecimal.ZERO), null, null, null);
            }
            case CASHBACK -> {
                BigDecimal cashback = scaleMoney(coupon.getDiscountValue());
                yield new BenefitCalculation(cashback, normalizedOrder, null, null, null);
            }
            case POINTS_BONUS -> new BenefitCalculation(
                BigDecimal.ZERO,
                normalizedOrder,
                coupon.getDiscountValue(),
                null,
                null
            );
            case FREE_ITEM -> new BenefitCalculation(
                BigDecimal.ZERO,
                normalizedOrder,
                null,
                null,
                null
            );
        };
    }

    private CouponRedeemResponse toReplayResponse(
        CouponRedemption redemption,
        String tenantId,
        String programmeUid,
        String customerId
    ) {
        CouponRedeemResponse response = new CouponRedeemResponse();
        response.setStatus("SUCCESS");
        response.setRedemptionUid(redemption.getRedemptionUid());
        response.setCouponUid(redemption.getCouponUid());
        response.setCouponCode(redemption.getCouponCode());
        response.setDiscountAmount(redemption.getDiscountAmount());
        response.setPointsCredited(redemption.getPointsCredited());
        response.setLedgerId(redemption.getLedgerId());
        response.setRedeemedAt(redemption.getRedeemedAt());
        response.setIdempotentReplay(true);
        if (redemption.getOrderAmount() != null && redemption.getDiscountAmount() != null) {
            response.setFinalAmount(redemption.getOrderAmount().subtract(redemption.getDiscountAmount()).max(BigDecimal.ZERO));
        }
        couponRepository.findByTenantIdAndCouponUid(tenantId, redemption.getCouponUid())
            .ifPresent(c -> response.setCouponType(c.getCouponType()));
        if (redemption.getPointsCredited() != null) {
            RewardBalanceResponse balance = rewardIssuanceService.getBalance(
                tenantId, normalizeProgramme(programmeUid), customerId
            );
            response.setNewBalance(balance.getBalance());
        }
        return response;
    }

    private static CouponValidateResponse invalid(CouponValidationReason reason, String message) {
        CouponValidateResponse response = new CouponValidateResponse();
        response.setValid(false);
        response.setStatus("INVALID");
        response.setReason(reason);
        response.setMessage(message);
        return response;
    }

    private static CouponValidationReason mapInactiveReason(CouponStatus status) {
        return switch (status) {
            case EXPIRED -> CouponValidationReason.EXPIRED;
            case EXHAUSTED -> CouponValidationReason.EXHAUSTED;
            default -> CouponValidationReason.INACTIVE;
        };
    }

    private static BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record BenefitCalculation(
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        BigDecimal pointsToCredit,
        CouponValidationReason reason,
        String message
    ) {}
}
