package com.loyaltyos.onboarding.analytics.service;

import com.loyaltyos.onboarding.analytics.repository.TierHistoryRepository;
import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TierHistoryService {

    private final TierHistoryRepository tierHistoryRepository;

    public TierHistoryService(TierHistoryRepository tierHistoryRepository) {
        this.tierHistoryRepository = Objects.requireNonNull(tierHistoryRepository, "tierHistoryRepository");
    }

    @Transactional
    public void recordIfTierChanged(
        String tenantId,
        String programmeUid,
        String customerId,
        Optional<TierDefinition> previousTier,
        Optional<TierDefinition> newTier,
        BigDecimal newBalance
    ) {
        TierDefinition next = newTier.orElse(null);
        if (next == null) {
            return;
        }
        String previousUid = previousTier.map(TierDefinition::getTierUid).orElse(null);
        if (next.getTierUid().equals(previousUid)) {
            return;
        }
        tierHistoryRepository.insertTierHistory(
            tenantId,
            programmeUid,
            customerId,
            previousUid,
            previousTier.map(TierDefinition::getName).orElse(null),
            next.getTierUid(),
            next.getName(),
            newBalance,
            "EARN_POINTS"
        );
    }
}
