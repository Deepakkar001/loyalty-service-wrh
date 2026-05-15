package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.rewards.config.RewardEngineProperties;
import com.loyaltyos.onboarding.rewards.entity.BalanceReconciliationLog;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCache;
import com.loyaltyos.onboarding.rewards.repository.BalanceReconciliationLogRepository;
import com.loyaltyos.onboarding.rewards.repository.CustomerBalanceCacheRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class BalanceReconciliationService {

    private final CustomerBalanceCacheRepository balanceCacheRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final BalanceReconciliationLogRepository balanceReconciliationLogRepository;
    private final RewardEngineProperties rewardEngineProperties;

    public BalanceReconciliationService(
        CustomerBalanceCacheRepository balanceCacheRepository,
        PointsLedgerRepository pointsLedgerRepository,
        BalanceReconciliationLogRepository balanceReconciliationLogRepository,
        RewardEngineProperties rewardEngineProperties
    ) {
        this.balanceCacheRepository = Objects.requireNonNull(balanceCacheRepository, "balanceCacheRepository");
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.balanceReconciliationLogRepository = Objects.requireNonNull(
            balanceReconciliationLogRepository,
            "balanceReconciliationLogRepository"
        );
        this.rewardEngineProperties = Objects.requireNonNull(rewardEngineProperties, "rewardEngineProperties");
    }

    /**
     * Samples distinct tenants and a page of cache rows per tenant; logs non-zero variance and optionally aligns cache to ledger.
     */
    public void runReconciliationSample() {
        int batch = Math.max(1, rewardEngineProperties.getReconciliationCustomerBatchLimit());
        List<String> tenants = balanceCacheRepository.findDistinctTenantIdsLimited(batch);
        for (String tenantId : tenants) {
            if (tenantId == null || tenantId.isBlank()) {
                continue;
            }
            Slice<CustomerBalanceCache> slice = balanceCacheRepository.findByTenantId(tenantId, PageRequest.of(0, batch));
            for (CustomerBalanceCache row : slice) {
                reconcileOneRow(row);
            }
        }
    }

    private void reconcileOneRow(CustomerBalanceCache row) {
        String tenantId = row.getTenantId();
        String programmeUid = row.getProgrammeUid();
        String customerId = row.getCustomerId();
        BigDecimal cached = row.getBalance() == null ? BigDecimal.ZERO : row.getBalance();
        BigDecimal expected = pointsLedgerRepository.sumSignedPointsForCustomer(tenantId, programmeUid, customerId);
        if (expected == null) {
            expected = BigDecimal.ZERO;
        }
        BigDecimal variance = expected.subtract(cached);
        if (variance.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BalanceReconciliationLog log = new BalanceReconciliationLog();
        log.setTenantId(tenantId);
        log.setProgrammeUid(programmeUid);
        log.setCustomerId(customerId);
        log.setExpectedBalance(expected);
        log.setCachedBalance(cached);
        log.setVariance(variance);

        if (rewardEngineProperties.isReconciliationAutoFixEnabled()) {
            int updated = balanceCacheRepository.setBalanceExact(tenantId, programmeUid, customerId, expected);
            log.setReconciliationAction(
                updated > 0 ? BalanceReconciliationLog.ACTION_UPDATED_CACHE : BalanceReconciliationLog.ACTION_MANUAL_REVIEW
            );
        } else {
            log.setReconciliationAction(BalanceReconciliationLog.ACTION_MANUAL_REVIEW);
        }
        balanceReconciliationLogRepository.save(log);
    }
}
