package com.loyaltyos.rewards.service;

import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.rewards.dto.RedemptionLimits;
import com.loyaltyos.rewards.dto.RedemptionRequest;
import com.loyaltyos.rewards.dto.RedemptionResult;
import com.loyaltyos.rewards.dto.RedemptionValidationResult;
import com.loyaltyos.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.exception.RewardRedemptionLimitExceededException;
import com.loyaltyos.rewards.exception.RewardRedemptionValidationException;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import com.loyaltyos.rules.entity.PointsLedger;
import com.loyaltyos.rules.enums.LedgerEntryType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class RewardRedemptionService {

    private static final int MAX_POINTS_SCALE = 4;
    private static final String STATUS_IDEMPOTENT_REPLAY = "IDEMPOTENT_REPLAY";
    private static final String IDEMPOTENT_REPLAY_MESSAGE =
        "This redemption was already processed; no additional points were debited.";

    private final RewardIssuanceService rewardIssuanceService;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheSyncService customerBalanceCacheSyncService;
    private final ProgrammeRedemptionConfigResolver redemptionConfigResolver;
    private final RewardCatalogService rewardCatalogService;

    public RewardRedemptionService(
        RewardIssuanceService rewardIssuanceService,
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheSyncService customerBalanceCacheSyncService,
        ProgrammeRedemptionConfigResolver redemptionConfigResolver,
        RewardCatalogService rewardCatalogService
    ) {
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.customerBalanceCacheSyncService = Objects.requireNonNull(
            customerBalanceCacheSyncService,
            "customerBalanceCacheSyncService"
        );
        this.redemptionConfigResolver = Objects.requireNonNull(redemptionConfigResolver, "redemptionConfigResolver");
        this.rewardCatalogService = Objects.requireNonNull(rewardCatalogService, "rewardCatalogService");
    }

    @Transactional(readOnly = true)
    public RedemptionValidationResult validateRedemption(String tenantId, RedemptionRequest request) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(request, "request");
        String programmeUid = normalizeProgramme(request.getProgrammeUid());
        String customerId = request.getCustomerId().trim();
        ResolvedRedemption resolved = resolvePointsAndCatalog(tenantId, programmeUid, request);
        BigDecimal points = resolved.points();

        Map<String, String> errors = new LinkedHashMap<>(resolved.errors());
        errors.putAll(validateBusinessRules(tenantId, programmeUid, customerId, points, request.getOrderAmount()));
        RewardBalanceResponse balance = rewardIssuanceService.getBalance(tenantId, programmeUid, customerId);
        BigDecimal available = balance.getBalance() == null ? BigDecimal.ZERO : balance.getBalance();

        RedemptionValidationResult result = new RedemptionValidationResult();
        result.setRedemptionId(request.getRedemptionId());
        result.setTimestamp(Instant.now());
        result.setPointsToRedeem(points);
        result.setCurrentBalance(available);
        result.setNote("Dry-run validation only — no points debited.");

        if (points.compareTo(available) > 0) {
            errors.put("pointsToRedeem", "Insufficient balance: available " + available.toPlainString());
        }

        applyCatalogToValidation(result, resolved.catalogItem());

        if (errors.isEmpty()) {
            result.setStatus("VALIDATION_SUCCESS");
            result.setValid(true);
        } else {
            result.setStatus("VALIDATION_FAILED");
            result.setValid(false);
            result.setFieldErrors(errors);
        }
        return result;
    }

    @Transactional
    public RedemptionResult redeem(String tenantId, RedemptionRequest request) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(request, "request");

        String programmeUid = normalizeProgramme(request.getProgrammeUid());
        String customerId = request.getCustomerId().trim();
        String redemptionId = request.getRedemptionId().trim();
        ResolvedRedemption resolved = resolvePointsAndCatalog(tenantId, programmeUid, request);
        BigDecimal points = resolved.points();
        String idempotencyKey = toIdempotencyKey(redemptionId);

        Optional<PointsLedger> existing = pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            tenantId, customerId, idempotencyKey
        );
        if (existing.isPresent()) {
            return buildReplayResult(tenantId, programmeUid, customerId, redemptionId, existing.get());
        }

        Map<String, String> errors = new LinkedHashMap<>(resolved.errors());
        errors.putAll(validateBusinessRules(tenantId, programmeUid, customerId, points, request.getOrderAmount()));
        RewardBalanceResponse balance = rewardIssuanceService.getBalance(tenantId, programmeUid, customerId);
        BigDecimal available = balance.getBalance() == null ? BigDecimal.ZERO : balance.getBalance();
        if (points.compareTo(available) > 0) {
            errors.put("pointsToRedeem", "Insufficient balance: available " + available.toPlainString());
        }
        if (!errors.isEmpty()) {
            if (points.compareTo(available) > 0) {
                throw new RewardInsufficientBalanceException(available, points);
            }
            if (containsProgrammeLimitViolation(errors)) {
                throw new RewardRedemptionLimitExceededException("Redemption exceeds programme limits", errors);
            }
            throw new RewardRedemptionValidationException("Redemption validation failed", errors);
        }

        String description = buildRedemptionDescription(redemptionId, resolved.catalogItem());
        PointsLedger debit = PointsLedger.builder()
            .tenantId(tenantId)
            .customerId(customerId)
            .programmeUid(programmeUid)
            .idempotencyKey(idempotencyKey)
            .entryType(LedgerEntryType.DEBIT)
            .points(points)
            .sourceEventId(redemptionId)
            .description(description)
            .createdBy("INTEGRATION_API")
            .build();

        PointsLedger saved = pointsLedgerRepository.save(debit);
        pointsLedgerRepository.flush();
        customerBalanceCacheSyncService.decrementOrRealignToLedger(tenantId, programmeUid, customerId, points);

        RedemptionResult result = new RedemptionResult();
        result.setStatus("SUCCESS");
        result.setRedemptionId(redemptionId);
        result.setCustomerId(customerId);
        result.setProgrammeUid(programmeUid);
        result.setPointsRedeemed(points);
        result.setLedgerId(saved.getId());
        result.setIdempotentReplay(false);
        result.setTimestamp(Instant.now());
        result.setNewBalance(rewardIssuanceService.getBalance(tenantId, programmeUid, customerId).getBalance());
        applyCatalogToResult(result, resolved.catalogItem());
        return result;
    }

    private RedemptionResult buildReplayResult(
        String tenantId,
        String programmeUid,
        String customerId,
        String redemptionId,
        PointsLedger row
    ) {
        if (row.getEntryType() != LedgerEntryType.DEBIT) {
            throw new RewardRedemptionValidationException(
                "Idempotency key already used by a non-DEBIT ledger row."
            );
        }
        RedemptionResult result = new RedemptionResult();
        result.setStatus(STATUS_IDEMPOTENT_REPLAY);
        result.setRedemptionId(redemptionId);
        result.setCustomerId(customerId);
        result.setProgrammeUid(programmeUid);
        result.setPointsRedeemed(row.getPoints());
        result.setLedgerId(row.getId());
        result.setIdempotentReplay(true);
        result.setMessage(IDEMPOTENT_REPLAY_MESSAGE);
        result.setTimestamp(Instant.now());
        result.setNewBalance(rewardIssuanceService.getBalance(tenantId, programmeUid, customerId).getBalance());
        return result;
    }

    private Map<String, String> validateBusinessRules(
        String tenantId,
        String programmeUid,
        String customerId,
        BigDecimal points,
        BigDecimal orderAmount
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (customerId.isBlank()) {
            errors.put("customerId", "customerId is required");
        }
        if (points.signum() <= 0) {
            errors.put("pointsToRedeem", "pointsToRedeem must be positive");
        }

        RedemptionLimits limits = redemptionConfigResolver.resolve(tenantId, programmeUid);
        if (limits.minRedemptionPoints() != null && points.compareTo(limits.minRedemptionPoints()) < 0) {
            errors.put(
                "pointsToRedeem",
                "Minimum redemption is " + limits.minRedemptionPoints().toPlainString() + " points"
            );
        }
        if (limits.maxRedemptionPctPerTxn() != null && orderAmount != null && orderAmount.signum() > 0) {
            BigDecimal maxPoints = orderAmount
                .multiply(limits.maxRedemptionPctPerTxn())
                .divide(BigDecimal.valueOf(100), MAX_POINTS_SCALE, RoundingMode.HALF_UP);
            if (points.compareTo(maxPoints) > 0) {
                errors.put(
                    "pointsToRedeem",
                    "Exceeds max redemption percent per transaction (" + limits.maxRedemptionPctPerTxn().toPlainString() + "%)"
                );
            }
        }
        return errors;
    }

    private static boolean containsProgrammeLimitViolation(Map<String, String> errors) {
        for (String message : errors.values()) {
            if (message == null) {
                continue;
            }
            if (message.startsWith("Minimum redemption is ")
                || message.startsWith("Exceeds max redemption percent per transaction")) {
                return true;
            }
        }
        return false;
    }

    private static String toIdempotencyKey(String redemptionId) {
        return "redeem:" + redemptionId;
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid;
    }

    private ResolvedRedemption resolvePointsAndCatalog(String tenantId, String programmeUid, RedemptionRequest request) {
        RewardCatalogService.CatalogRedemptionResolution resolution = rewardCatalogService.resolveRedemption(
            tenantId,
            programmeUid,
            request.getCatalogRewardUid(),
            request.getPointsToRedeem()
        );
        if (!resolution.isValid()) {
            return new ResolvedRedemption(null, BigDecimal.ZERO, resolution.catalogItem(), resolution.errors());
        }
        BigDecimal points = resolution.resolvedPoints();
        if (points == null || points.signum() <= 0) {
            Map<String, String> errors = new LinkedHashMap<>(resolution.errors());
            errors.putIfAbsent("pointsToRedeem", "pointsToRedeem is required when catalogRewardUid is omitted");
            return new ResolvedRedemption(points, BigDecimal.ZERO, resolution.catalogItem(), errors);
        }
        return new ResolvedRedemption(
            points.setScale(MAX_POINTS_SCALE, RoundingMode.HALF_UP),
            points,
            resolution.catalogItem(),
            resolution.errors()
        );
    }

    private static String buildRedemptionDescription(String redemptionId, RewardCatalogItem catalogItem) {
        if (catalogItem == null) {
            return "REDEMPTION " + redemptionId;
        }
        return "REDEMPTION " + redemptionId + " catalog=" + catalogItem.rewardUid() + " type=" + catalogItem.rewardType();
    }

    private static void applyCatalogToValidation(RedemptionValidationResult result, RewardCatalogItem item) {
        if (item == null) {
            return;
        }
        result.setCatalogRewardUid(item.rewardUid());
        result.setCatalogRewardName(item.name());
        result.setCatalogRewardType(item.rewardType());
    }

    private static void applyCatalogToResult(RedemptionResult result, RewardCatalogItem item) {
        if (item == null) {
            return;
        }
        result.setCatalogRewardUid(item.rewardUid());
        result.setCatalogRewardName(item.name());
        result.setCatalogRewardType(item.rewardType());
    }

    private record ResolvedRedemption(
        BigDecimal points,
        BigDecimal rawPoints,
        RewardCatalogItem catalogItem,
        Map<String, String> errors
    ) {}
}
