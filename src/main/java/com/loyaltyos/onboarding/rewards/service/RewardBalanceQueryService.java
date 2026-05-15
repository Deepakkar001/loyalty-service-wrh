package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.rewards.dto.RewardBalanceDetailResponse;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCache;
import com.loyaltyos.onboarding.rewards.entity.CustomerBalanceCacheId;
import com.loyaltyos.onboarding.rewards.repository.CustomerBalanceCacheRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardBalanceQueryService {

    private final PointsLedgerRepository pointsLedgerRepository;
    private final CustomerBalanceCacheRepository balanceCacheRepository;

    public RewardBalanceQueryService(
        PointsLedgerRepository pointsLedgerRepository,
        CustomerBalanceCacheRepository balanceCacheRepository
    ) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.balanceCacheRepository = Objects.requireNonNull(balanceCacheRepository, "balanceCacheRepository");
    }

    @Transactional(readOnly = true)
    public RewardBalanceDetailResponse getBalanceDetail(String tenantId, String programmeUid, String customerId) {
        String p = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid;
        BigDecimal ledgerSum = pointsLedgerRepository.sumSignedPointsForCustomer(tenantId, p, customerId);
        if (ledgerSum == null) {
            ledgerSum = BigDecimal.ZERO;
        }
        CustomerBalanceCacheId id = new CustomerBalanceCacheId(tenantId, p, customerId);
        BigDecimal cache = balanceCacheRepository.findById(id)
            .map(CustomerBalanceCache::getBalance)
            .orElse(BigDecimal.ZERO);
        if (cache == null) {
            cache = BigDecimal.ZERO;
        }
        Instant now = Instant.now();
        Instant until = now.plus(7, ChronoUnit.DAYS);
        BigDecimal expiring = pointsLedgerRepository.sumCreditPointsExpiringBetween(
            tenantId, p, customerId, now, until
        );
        if (expiring == null) {
            expiring = BigDecimal.ZERO;
        }
        RewardBalanceDetailResponse r = new RewardBalanceDetailResponse();
        r.setTenantId(tenantId);
        r.setProgrammeUid(p);
        r.setCustomerId(customerId);
        r.setCachedBalance(cache);
        r.setLedgerDerivedBalance(ledgerSum);
        r.setVariance(ledgerSum.subtract(cache));
        r.setExpiringWithin7Days(expiring);
        return r;
    }
}
