package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.rules.entity.PointsLedger;
import com.loyaltyos.onboarding.rules.enums.LedgerEntryType;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One credit expiry attempt per transaction — isolates failures and keeps locks short.
 */
@Service
public class PointsExpiryTxnService {

    private static final Logger log = LoggerFactory.getLogger(PointsExpiryTxnService.class);

    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheSyncService customerBalanceCacheSyncService;

    public PointsExpiryTxnService(
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheSyncService customerBalanceCacheSyncService
    ) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.customerBalanceCacheSyncService = Objects.requireNonNull(
            customerBalanceCacheSyncService,
            "customerBalanceCacheSyncService"
        );
    }

    /**
     * @return true if a new EXPIRE row was written for this tick
     */
    @Transactional
    public boolean tryExpireCreditRow(Long creditLedgerId, Instant now) {
        PointsLedger credit = pointsLedgerRepository.findById(creditLedgerId).orElse(null);
        if (credit == null) {
            return false;
        }
        if (credit.getEntryType() != LedgerEntryType.CREDIT) {
            return false;
        }
        if (credit.getExpiresAt() == null || !credit.getExpiresAt().isBefore(now)) {
            return false;
        }

        String tenantId = credit.getTenantId();
        String customerId = credit.getCustomerId();
        String idem = "exp:" + credit.getId();
        if (pointsLedgerRepository.existsByTenantIdAndCustomerIdAndIdempotencyKey(tenantId, customerId, idem)) {
            return false;
        }

        PointsLedger expireRow = PointsLedger.builder()
            .tenantId(tenantId)
            .customerId(customerId)
            .programmeUid(credit.getProgrammeUid())
            .idempotencyKey(idem)
            .entryType(LedgerEntryType.EXPIRE)
            .points(credit.getPoints())
            .sourceRuleId(credit.getSourceRuleId())
            .sourceEventId(credit.getSourceEventId())
            .sourceCampaignId(credit.getSourceCampaignId())
            .expiresAt(null)
            .description("EXPIRE of CREDIT ledger id=" + credit.getId())
            .createdBy("REWARD_ENGINE")
            .build();

        try {
            pointsLedgerRepository.save(expireRow);
            pointsLedgerRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.debug("Expiry idempotency race for credit {}: {}", creditLedgerId, ex.getMessage());
            return false;
        }

        customerBalanceCacheSyncService.decrementOrRealignToLedger(
            tenantId,
            credit.getProgrammeUid(),
            customerId,
            credit.getPoints()
        );
        return true;
    }
}
