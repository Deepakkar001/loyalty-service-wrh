package com.loyaltyos.rewards.service;

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

    private final RewardIssuanceService rewardIssuanceService;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheSyncService customerBalanceCacheSyncService;
    private final ProgrammeRedemptionConfigResolver redemptionConfigResolver;

    public RewardRedemptionService(
        RewardIssuanceService rewardIssuanceService,
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheSyncService customerBalanceCacheSyncService,
        ProgrammeRedemptionConfigResolver redemptionConfigResolver
    ) {
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.customerBalanceCacheSyncService = Objects.requireNonNull(
            customerBalanceCacheSyncService,
            "customerBalanceCacheSyncService"
        );
        this.redemptionConfigResolver = Objects.requireNonNull(redemptionConfigResolver, "redemptionConfigResolver");
    }

    @Transactional(readOnly = true)
    public RedemptionValidationResult validateRedemption(String tenantId, RedemptionRequest request) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(request, "request");
        String programmeUid = normalizeProgramme(request.getProgrammeUid());
        String customerId = request.getCustomerId().trim();
        BigDecimal points = normalizePoints(request.getPointsToRedeem());

        Map<String, String> errors = validateBusinessRules(tenantId, programmeUid, customerId, points, request.getOrderAmount());
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
        BigDecimal points = normalizePoints(request.getPointsToRedeem());
        String idempotencyKey = toIdempotencyKey(redemptionId);

        Optional<PointsLedger> existing = pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            tenantId, customerId, idempotencyKey
        );
        if (existing.isPresent()) {
            return buildReplayResult(tenantId, programmeUid, customerId, redemptionId, existing.get());
        }

        Map<String, String> errors = validateBusinessRules(tenantId, programmeUid, customerId, points, request.getOrderAmount());
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

        PointsLedger debit = PointsLedger.builder()
            .tenantId(tenantId)
            .customerId(customerId)
            .programmeUid(programmeUid)
            .idempotencyKey(idempotencyKey)
            .entryType(LedgerEntryType.DEBIT)
            .points(points)
            .sourceEventId(redemptionId)
            .description("REDEMPTION " + redemptionId)
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
        result.setStatus("SUCCESS");
        result.setRedemptionId(redemptionId);
        result.setCustomerId(customerId);
        result.setProgrammeUid(programmeUid);
        result.setPointsRedeemed(row.getPoints());
        result.setLedgerId(row.getId());
        result.setIdempotentReplay(true);
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

    private static BigDecimal normalizePoints(BigDecimal points) {
        if (points == null) {
            throw new RewardRedemptionValidationException("pointsToRedeem is required");
        }
        return points.setScale(MAX_POINTS_SCALE, RoundingMode.HALF_UP);
    }
}
