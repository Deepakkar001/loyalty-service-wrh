package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.rules.entity.PointsLedger;
import com.loyaltyos.onboarding.rules.enums.LedgerEntryType;
import com.loyaltyos.onboarding.rewards.dto.RewardReverseRequest;
import com.loyaltyos.onboarding.rewards.dto.RewardReverseResponse;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCache;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCacheId;
import com.loyaltyos.onboarding.rewards.exception.RewardCreditAlreadyReversedException;
import com.loyaltyos.onboarding.rewards.exception.RewardLedgerNotFoundException;
import com.loyaltyos.onboarding.rewards.exception.RewardReversalValidationException;
import com.loyaltyos.onboarding.rewards.repository.CustomerBalanceCacheRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardReversalService {

    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheRepository balanceCacheRepository;
    private final CustomerBalanceCacheSyncService customerBalanceCacheSyncService;

    public RewardReversalService(
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheRepository balanceCacheRepository,
        CustomerBalanceCacheSyncService customerBalanceCacheSyncService
    ) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.balanceCacheRepository = Objects.requireNonNull(balanceCacheRepository, "balanceCacheRepository");
        this.customerBalanceCacheSyncService = Objects.requireNonNull(
            customerBalanceCacheSyncService,
            "customerBalanceCacheSyncService"
        );
    }

    @Transactional
    public RewardReverseResponse reverse(String tenantId, RewardReverseRequest request) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(request, "request");

        String programmeUid = request.getProgrammeUid() == null || request.getProgrammeUid().isBlank()
            ? "default"
            : request.getProgrammeUid();
        String customerId = request.getCustomerId();
        String reversalKey = request.getReversalIdempotencyKey();
        Long creditLedgerId = request.getCreditLedgerId();

        if (reversalKey.length() > 128) {
            throw new RewardReversalValidationException("reversalIdempotencyKey exceeds 128 characters.");
        }

        RewardReverseResponse response = new RewardReverseResponse();
        response.setTenantId(tenantId);
        response.setProgrammeUid(programmeUid);
        response.setCustomerId(customerId);
        response.setCreditLedgerId(creditLedgerId);
        response.setProcessedAt(Instant.now());

        Optional<PointsLedger> existingReversal = pointsLedgerRepository.findFirstByTenantIdAndCustomerIdAndIdempotencyKey(
            tenantId,
            customerId,
            reversalKey
        );
        if (existingReversal.isPresent()) {
            PointsLedger row = existingReversal.get();
            if (row.getEntryType() != LedgerEntryType.REVERSAL) {
                throw new RewardReversalValidationException(
                    "Idempotency key already used by a non-REVERSAL ledger row."
                );
            }
            response.setReversalLedgerId(row.getId());
            response.setPointsReversed(row.getPoints());
            response.setIdempotentReplay(true);
            response.setNewBalance(readBalance(tenantId, programmeUid, customerId));
            response.setMessage("Idempotent replay: reversal already recorded.");
            return response;
        }

        PointsLedger credit = pointsLedgerRepository
            .findById(creditLedgerId)
            .orElseThrow(() -> new RewardLedgerNotFoundException("Credit ledger row not found: " + creditLedgerId));

        if (!tenantId.equals(credit.getTenantId())) {
            throw new RewardLedgerNotFoundException("Credit ledger row not found for this tenant.");
        }
        if (!customerId.equals(credit.getCustomerId())) {
            throw new RewardReversalValidationException("customerId does not match the credit ledger row.");
        }
        if (!programmeUid.equals(credit.getProgrammeUid())) {
            throw new RewardReversalValidationException("programmeUid does not match the credit ledger row.");
        }
        if (credit.getEntryType() != LedgerEntryType.CREDIT) {
            throw new RewardReversalValidationException("Only CREDIT rows can be reversed.");
        }

        String expKey = "exp:" + credit.getId();
        if (pointsLedgerRepository.existsByTenantIdAndCustomerIdAndIdempotencyKey(tenantId, customerId, expKey)) {
            throw new RewardReversalValidationException("This credit has already been expired; reversal is not allowed.");
        }

        if (pointsLedgerRepository.existsByTenantIdAndCustomerIdAndEntryTypeAndReversalOfLedgerId(
            tenantId,
            customerId,
            LedgerEntryType.REVERSAL,
            credit.getId()
        )) {
            throw new RewardCreditAlreadyReversedException(
                "This credit was already reversed under a different idempotency key."
            );
        }

        BigDecimal pts = credit.getPoints();
        if (pts == null || pts.signum() <= 0) {
            throw new RewardReversalValidationException("Credit points must be positive.");
        }

        PointsLedger reversal = PointsLedger.builder()
            .tenantId(tenantId)
            .customerId(customerId)
            .programmeUid(programmeUid)
            .idempotencyKey(reversalKey)
            .entryType(LedgerEntryType.REVERSAL)
            .points(pts)
            .sourceRuleId(credit.getSourceRuleId())
            .sourceEventId(credit.getSourceEventId())
            .sourceCampaignId(null)
            .reversalOfLedgerId(credit.getId())
            .expiresAt(null)
            .description("REVERSAL of CREDIT ledger id=" + credit.getId())
            .createdBy("REWARD_ENGINE")
            .build();

        PointsLedger saved = pointsLedgerRepository.save(reversal);
        pointsLedgerRepository.flush();

        customerBalanceCacheSyncService.decrementOrRealignToLedger(tenantId, programmeUid, customerId, pts);

        response.setReversalLedgerId(saved.getId());
        response.setPointsReversed(pts);
        response.setIdempotentReplay(false);
        response.setNewBalance(readBalance(tenantId, programmeUid, customerId));
        response.setMessage("Reversal recorded.");
        return response;
    }

    private BigDecimal readBalance(String tenantId, String programmeUid, String customerId) {
        CustomerBalanceCacheId id = new CustomerBalanceCacheId(tenantId, programmeUid, customerId);
        return balanceCacheRepository.findById(id)
            .map(CustomerBalanceCache::getBalance)
            .orElse(BigDecimal.ZERO);
    }
}
