package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.model.CustomerTierOutcome;
import com.loyaltyos.onboarding.entity.TierDefinition;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves tier UIDs and human-readable names for earn and integration responses.
 * <ul>
 *   <li>{@code tierBeforeUid}: explicit request tier when provided, otherwise tier from balance before earn.</li>
 *   <li>{@code tierAfterUid}: tier from balance after earn (falls back to {@code tierBeforeUid}).</li>
 *   <li>{@code tierBeforeName} / {@code tierAfterName}: programme tier display names (falls back to UID).</li>
 * </ul>
 */
@Component
public class CustomerTierOutcomeResolver {

    private final TierResolver tierResolver;
    private final TierDefinitionRepository tierDefinitionRepository;

    public CustomerTierOutcomeResolver(
        TierResolver tierResolver,
        TierDefinitionRepository tierDefinitionRepository
    ) {
        this.tierResolver = Objects.requireNonNull(tierResolver, "tierResolver");
        this.tierDefinitionRepository = Objects.requireNonNull(
            tierDefinitionRepository, "tierDefinitionRepository"
        );
    }

    public CustomerTierOutcome resolve(
        String tenantId,
        String programmeUid,
        String requestedTierUid,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter
    ) {
        String programme = normalizeProgramme(programmeUid);
        BigDecimal beforeBal = balanceBefore != null ? balanceBefore : BigDecimal.ZERO;
        BigDecimal afterBal = balanceAfter != null ? balanceAfter : beforeBal;

        TierDefinition beforeFromBalance = tierResolver
            .resolveTierForBalance(tenantId, programme, beforeBal)
            .orElse(null);
        TierDefinition afterFromBalance = tierResolver
            .resolveTierForBalance(tenantId, programme, afterBal)
            .orElse(null);

        String tierBeforeUid = normalizeTierUid(requestedTierUid);
        if (tierBeforeUid == null && beforeFromBalance != null) {
            tierBeforeUid = beforeFromBalance.getTierUid();
        }

        String tierAfterUid = afterFromBalance != null ? afterFromBalance.getTierUid() : tierBeforeUid;

        TierDefinition beforeMeta = resolveTierDefinition(
            tenantId, programme, tierBeforeUid, beforeFromBalance
        );
        TierDefinition afterMeta = resolveTierDefinition(
            tenantId, programme, tierAfterUid, afterFromBalance
        );

        String tierBeforeName = displayName(beforeMeta, tierBeforeUid);
        String tierAfterName = displayName(afterMeta, tierAfterUid);

        boolean tierChanged = tierBeforeUid != null && tierAfterUid != null
            ? !tierBeforeUid.equals(tierAfterUid)
            : !Objects.equals(tierBeforeUid, tierAfterUid);

        return new CustomerTierOutcome(
            tierBeforeUid,
            tierAfterUid,
            tierBeforeName,
            tierAfterName,
            tierChanged
        );
    }

    private TierDefinition resolveTierDefinition(
        String tenantId,
        String programmeUid,
        String tierUid,
        TierDefinition fromBalance
    ) {
        if (tierUid == null || tierUid.isBlank()) {
            return fromBalance;
        }
        if (fromBalance != null && tierUid.equals(fromBalance.getTierUid())) {
            return fromBalance;
        }
        return findTierByUid(tenantId, programmeUid, tierUid).orElse(fromBalance);
    }

    private Optional<TierDefinition> findTierByUid(String tenantId, String programmeUid, String tierUid) {
        List<TierDefinition> scoped = tierDefinitionRepository
            .findByTenantIdAndProgrammeUidOrderByRankOrderAsc(tenantId, programmeUid);
        Optional<TierDefinition> match = scoped.stream()
            .filter(t -> tierUid.equals(t.getTierUid()))
            .findFirst();
        if (match.isPresent()) {
            return match;
        }
        return tierDefinitionRepository.findByTenantIdOrderByRankOrderAsc(tenantId).stream()
            .filter(t -> tierUid.equals(t.getTierUid()))
            .findFirst();
    }

    private static String displayName(TierDefinition tier, String tierUid) {
        if (tier != null && tier.getName() != null && !tier.getName().isBlank()) {
            return tier.getName().trim();
        }
        return tierUid;
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }

    private static String normalizeTierUid(String tierUid) {
        if (tierUid == null || tierUid.isBlank()) {
            return null;
        }
        return tierUid.trim();
    }
}
