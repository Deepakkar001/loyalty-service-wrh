package com.loyaltyos.onboarding.analytics.service;

import com.loyaltyos.onboarding.domain.entity.TierDefinition;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TierResolver {

    private final TierDefinitionRepository tierDefinitionRepository;

    public TierResolver(TierDefinitionRepository tierDefinitionRepository) {
        this.tierDefinitionRepository = Objects.requireNonNull(tierDefinitionRepository, "tierDefinitionRepository");
    }

    public Optional<TierDefinition> resolveTierForBalance(
        String tenantId,
        String programmeUid,
        BigDecimal balance
    ) {
        if (balance == null) {
            return Optional.empty();
        }
        List<TierDefinition> tiers = tierDefinitionRepository
            .findByTenantIdAndProgrammeUidOrderByRankOrderAsc(tenantId, programmeUid);
        if (tiers.isEmpty()) {
            return Optional.empty();
        }
        TierDefinition best = null;
        for (TierDefinition tier : tiers) {
            if (balance.compareTo(tier.getEntryThreshold()) >= 0) {
                if (best == null || tier.getRankOrder() > best.getRankOrder()) {
                    best = tier;
                }
            }
        }
        if (best != null) {
            return Optional.of(best);
        }
        return tiers.stream().min(Comparator.comparing(TierDefinition::getRankOrder));
    }
}
