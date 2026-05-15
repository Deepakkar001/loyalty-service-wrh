package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCacheId;
import com.loyaltyos.onboarding.rewards.repository.CustomerBalanceCacheRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps {@code customer_balance_cache} aligned with the ledger when atomic decrements cannot apply
 * (missing row, or cache ahead of ledger). Ledger sum remains authoritative.
 */
@Service
public class CustomerBalanceCacheSyncService {

    private static final Logger log = LoggerFactory.getLogger(CustomerBalanceCacheSyncService.class);

    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheRepository balanceCacheRepository;

    public CustomerBalanceCacheSyncService(
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheRepository balanceCacheRepository
    ) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.balanceCacheRepository = Objects.requireNonNull(balanceCacheRepository, "balanceCacheRepository");
    }

    /**
     * Subtracts {@code delta} from cache when possible; otherwise overwrites or creates the cache row
     * from the signed ledger net for this customer and programme.
     */
    public void decrementOrRealignToLedger(
        String tenantId,
        String programmeUid,
        String customerId,
        BigDecimal delta
    ) {
        if (delta == null || delta.signum() <= 0) {
            return;
        }
        int n = balanceCacheRepository.decrementBalanceIfSufficient(tenantId, programmeUid, customerId, delta);
        if (n == 1) {
            return;
        }
        log.error(
            "Balance cache decrement skipped (tenant={}, programme={}, customer={}, delta={}); realigning cache to ledger sum.",
            tenantId,
            programmeUid,
            customerId,
            delta
        );
        syncCacheToLedgerSum(tenantId, programmeUid, customerId);
    }

    public void syncCacheToLedgerSum(String tenantId, String programmeUid, String customerId) {
        BigDecimal sum = pointsLedgerRepository.sumSignedPointsForCustomer(tenantId, programmeUid, customerId);
        if (sum == null) {
            sum = BigDecimal.ZERO;
        }
        CustomerBalanceCacheId id = new CustomerBalanceCacheId(tenantId, programmeUid, customerId);
        if (balanceCacheRepository.findById(id).isPresent()) {
            balanceCacheRepository.setBalanceExact(tenantId, programmeUid, customerId, sum);
        } else {
            balanceCacheRepository.incrementBalance(tenantId, programmeUid, customerId, sum);
        }
    }
}
