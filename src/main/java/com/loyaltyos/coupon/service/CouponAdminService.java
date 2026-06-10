package com.loyaltyos.coupon.service;

import com.loyaltyos.coupon.dto.CouponCreateRequest;
import com.loyaltyos.coupon.dto.CouponRedemptionListItem;
import com.loyaltyos.coupon.dto.CouponResponse;
import com.loyaltyos.coupon.dto.CouponUpdateRequest;
import com.loyaltyos.coupon.entity.Coupon;
import com.loyaltyos.coupon.entity.CouponRedemption;
import com.loyaltyos.coupon.enums.CouponStatus;
import com.loyaltyos.coupon.enums.CouponType;
import com.loyaltyos.coupon.enums.CouponUsageType;
import com.loyaltyos.coupon.exception.CouponAdminException;
import com.loyaltyos.coupon.exception.CouponNotFoundException;
import com.loyaltyos.coupon.model.CouponConstraints;
import com.loyaltyos.coupon.repository.CouponRedemptionRepository;
import com.loyaltyos.coupon.repository.CouponRepository;
import com.loyaltyos.coupon.support.CouponCodeNormalizer;
import com.loyaltyos.coupon.support.CouponConstraintsSupport;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponAdminService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final CouponConstraintsSupport constraintsSupport;

    public CouponAdminService(
        CouponRepository couponRepository,
        CouponRedemptionRepository redemptionRepository,
        CouponConstraintsSupport constraintsSupport
    ) {
        this.couponRepository = Objects.requireNonNull(couponRepository);
        this.redemptionRepository = Objects.requireNonNull(redemptionRepository);
        this.constraintsSupport = Objects.requireNonNull(constraintsSupport);
    }

    @Transactional
    public CouponResponse create(String tenantId, String createdBy, CouponCreateRequest request) {
        validateTypeConfig(request.getCouponType(), request.getDiscountValue(), request.getDiscountPct());
        if (request.getValidUntil() == null) {
            throw new CouponAdminException("validUntil is required");
        }
        if (request.getValidFrom() != null && request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new CouponAdminException("validFrom must be before validUntil");
        }

        String code = CouponCodeNormalizer.normalize(request.getCouponCode());
        if (code == null || code.isBlank()) {
            throw new CouponAdminException("couponCode is required");
        }
        couponRepository.findByTenantIdAndCouponCode(tenantId, code).ifPresent(c -> {
            throw new CouponAdminException("Coupon code already exists: " + code);
        });

        Instant now = Instant.now();
        Coupon coupon = new Coupon();
        coupon.setCouponUid(UUID.randomUUID().toString());
        coupon.setTenantId(tenantId);
        coupon.setProgrammeUid(normalizeProgramme(request.getProgrammeUid()));
        coupon.setName(request.getName().trim());
        coupon.setDescription(trimToNull(request.getDescription()));
        coupon.setCouponCode(code);
        coupon.setCouponType(request.getCouponType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setDiscountPct(request.getDiscountPct());
        coupon.setUsageType(request.getUsageType() != null ? request.getUsageType() : CouponUsageType.SINGLE_USE);
        coupon.setMaxRedemptions(resolveMaxRedemptions(request.getUsageType(), request.getMaxRedemptions()));
        coupon.setMaxRedemptionsPerCustomer(Math.max(1, request.getMaxRedemptionsPerCustomer()));
        coupon.setStackable(request.isStackable());
        coupon.setTargetCustomerId(trimToNull(request.getTargetCustomerId()));
        coupon.setCampaignUid(trimToNull(request.getCampaignUid()));
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setConstraintsJson(constraintsSupport.serialize(
            request.getConstraints() != null ? request.getConstraints() : new CouponConstraints()
        ));
        coupon.setCreatedBy(createdBy);
        coupon.setCreatedAt(now);
        coupon.setUpdatedAt(now);
        coupon.setStatus(request.isActivateImmediately() ? CouponStatus.ACTIVE : CouponStatus.DRAFT);

        try {
            return toResponse(couponRepository.save(coupon));
        } catch (DataIntegrityViolationException e) {
            throw new CouponAdminException("Coupon code already exists: " + code);
        }
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> list(String tenantId, String programmeUid) {
        List<Coupon> rows = programmeUid == null || programmeUid.isBlank()
            ? couponRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            : couponRepository.findByTenantIdAndProgrammeUidOrderByCreatedAtDesc(
                tenantId, normalizeProgramme(programmeUid)
            );
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse get(String tenantId, String couponUid) {
        return toResponse(requireCoupon(tenantId, couponUid));
    }

    @Transactional
    public CouponResponse update(String tenantId, String couponUid, CouponUpdateRequest request) {
        Coupon coupon = requireCoupon(tenantId, couponUid);
        if (coupon.getStatus() == CouponStatus.REVOKED || coupon.getStatus() == CouponStatus.EXHAUSTED) {
            throw new CouponAdminException("Cannot update coupon in status " + coupon.getStatus());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            coupon.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            coupon.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getCouponType() != null) {
            coupon.setCouponType(request.getCouponType());
        }
        if (request.getDiscountValue() != null) {
            coupon.setDiscountValue(request.getDiscountValue());
        }
        if (request.getDiscountPct() != null) {
            coupon.setDiscountPct(request.getDiscountPct());
        }
        validateTypeConfig(coupon.getCouponType(), coupon.getDiscountValue(), coupon.getDiscountPct());
        if (request.getUsageType() != null) {
            coupon.setUsageType(request.getUsageType());
        }
        if (request.getMaxRedemptions() != null) {
            coupon.setMaxRedemptions(Math.max(1, request.getMaxRedemptions()));
        }
        if (request.getMaxRedemptionsPerCustomer() != null) {
            coupon.setMaxRedemptionsPerCustomer(Math.max(1, request.getMaxRedemptionsPerCustomer()));
        }
        if (request.getStackable() != null) {
            coupon.setStackable(request.getStackable());
        }
        if (request.getTargetCustomerId() != null) {
            coupon.setTargetCustomerId(trimToNull(request.getTargetCustomerId()));
        }
        if (request.getCampaignUid() != null) {
            coupon.setCampaignUid(trimToNull(request.getCampaignUid()));
        }
        if (request.getValidFrom() != null) {
            coupon.setValidFrom(request.getValidFrom());
        }
        if (request.getValidUntil() != null) {
            coupon.setValidUntil(request.getValidUntil());
        }
        if (request.getConstraints() != null) {
            coupon.setConstraintsJson(constraintsSupport.serialize(request.getConstraints()));
        }
        coupon.setUpdatedAt(Instant.now());
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse activate(String tenantId, String couponUid) {
        Coupon coupon = requireCoupon(tenantId, couponUid);
        if (coupon.getStatus() == CouponStatus.REVOKED) {
            throw new CouponAdminException("Revoked coupons cannot be activated");
        }
        if (coupon.getValidUntil().isBefore(Instant.now())) {
            throw new CouponAdminException("Cannot activate an expired coupon");
        }
        validateTypeConfig(coupon.getCouponType(), coupon.getDiscountValue(), coupon.getDiscountPct());
        coupon.setStatus(CouponStatus.ACTIVE);
        coupon.setUpdatedAt(Instant.now());
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse revoke(String tenantId, String couponUid) {
        Coupon coupon = requireCoupon(tenantId, couponUid);
        coupon.setStatus(CouponStatus.REVOKED);
        coupon.setUpdatedAt(Instant.now());
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional(readOnly = true)
    public List<CouponRedemptionListItem> listRedemptions(String tenantId, String couponUid) {
        requireCoupon(tenantId, couponUid);
        return redemptionRepository.findByTenantIdAndCouponUidOrderByRedeemedAtDesc(tenantId, couponUid)
            .stream()
            .map(this::toRedemptionItem)
            .toList();
    }

    private Coupon requireCoupon(String tenantId, String couponUid) {
        return couponRepository.findByTenantIdAndCouponUid(tenantId, couponUid)
            .orElseThrow(() -> new CouponNotFoundException("Coupon not found: " + couponUid));
    }

    private CouponResponse toResponse(Coupon coupon) {
        CouponResponse response = new CouponResponse();
        response.setCouponUid(coupon.getCouponUid());
        response.setProgrammeUid(coupon.getProgrammeUid());
        response.setName(coupon.getName());
        response.setDescription(coupon.getDescription());
        response.setCouponCode(coupon.getCouponCode());
        response.setCouponType(coupon.getCouponType());
        response.setDiscountValue(coupon.getDiscountValue());
        response.setDiscountPct(coupon.getDiscountPct());
        response.setStatus(coupon.getStatus());
        response.setUsageType(coupon.getUsageType());
        response.setMaxRedemptions(coupon.getMaxRedemptions());
        response.setRedemptionCount(coupon.getRedemptionCount());
        response.setMaxRedemptionsPerCustomer(coupon.getMaxRedemptionsPerCustomer());
        response.setStackable(coupon.isStackable());
        response.setTargetCustomerId(coupon.getTargetCustomerId());
        response.setCampaignUid(coupon.getCampaignUid());
        response.setValidFrom(coupon.getValidFrom());
        response.setValidUntil(coupon.getValidUntil());
        response.setConstraints(constraintsSupport.parse(coupon.getConstraintsJson()));
        response.setCreatedAt(coupon.getCreatedAt());
        response.setUpdatedAt(coupon.getUpdatedAt());
        return response;
    }

    private CouponRedemptionListItem toRedemptionItem(CouponRedemption row) {
        CouponRedemptionListItem item = new CouponRedemptionListItem();
        item.setRedemptionUid(row.getRedemptionUid());
        item.setCustomerId(row.getCustomerId());
        item.setOrderId(row.getOrderId());
        item.setChannel(row.getChannel());
        item.setOrderAmount(row.getOrderAmount());
        item.setDiscountAmount(row.getDiscountAmount());
        item.setPointsCredited(row.getPointsCredited());
        item.setStatus(row.getStatus().name());
        item.setRedeemedAt(row.getRedeemedAt());
        return item;
    }

    static void validateTypeConfig(CouponType type, java.math.BigDecimal discountValue, java.math.BigDecimal discountPct) {
        switch (type) {
            case FIXED_DISCOUNT, CASHBACK, POINTS_BONUS -> {
                if (discountValue == null || discountValue.signum() <= 0) {
                    throw new CouponAdminException(type + " requires discountValue > 0");
                }
            }
            case FREE_ITEM -> {
                // Benefit is defined via constraints.freeItemSku / freeItemLabel — no monetary value required.
            }
            case PCT_DISCOUNT -> {
                if (discountPct == null || discountPct.signum() <= 0) {
                    throw new CouponAdminException("PCT_DISCOUNT requires discountPct > 0");
                }
            }
        }
    }

    private static int resolveMaxRedemptions(CouponUsageType usageType, int requested) {
        if (usageType == CouponUsageType.SINGLE_USE) {
            return 1;
        }
        return Math.max(1, requested);
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
}
