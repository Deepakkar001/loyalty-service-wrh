package com.loyaltyos.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.loyaltyos.analytics.model.CustomerTierOutcome;
import com.loyaltyos.onboarding.entity.TierDefinition;
import com.loyaltyos.onboarding.repository.TierDefinitionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerTierOutcomeResolverTest {

    @Mock
    private TierResolver tierResolver;

    @Mock
    private TierDefinitionRepository tierDefinitionRepository;

    @InjectMocks
    private CustomerTierOutcomeResolver resolver;

    @Test
    void resolve_withoutRequestedTier_usesBalanceDerivedBeforeAndAfterWithNames() {
        TierDefinition bronze = tier("uuid-bronze", "Bronze", 1);
        TierDefinition silver = tier("uuid-silver", "Silver", 2);
        when(tierResolver.resolveTierForBalance(eq("t1"), eq("default"), eq(new BigDecimal("0"))))
            .thenReturn(Optional.of(bronze));
        when(tierResolver.resolveTierForBalance(eq("t1"), eq("default"), eq(new BigDecimal("4000"))))
            .thenReturn(Optional.of(silver));

        CustomerTierOutcome outcome = resolver.resolve(
            "t1", "default", null, BigDecimal.ZERO, new BigDecimal("4000")
        );

        assertThat(outcome.tierBeforeUid()).isEqualTo("uuid-bronze");
        assertThat(outcome.tierAfterUid()).isEqualTo("uuid-silver");
        assertThat(outcome.tierBeforeName()).isEqualTo("Bronze");
        assertThat(outcome.tierAfterName()).isEqualTo("Silver");
        assertThat(outcome.tierChanged()).isTrue();
    }

    @Test
    void resolve_withRequestedTierUid_looksUpDisplayName() {
        TierDefinition silver = tier("uuid-silver", "Silver", 2);
        TierDefinition bronze = tier("uuid-bronze", "Bronze", 1);
        when(tierResolver.resolveTierForBalance(eq("t1"), eq("default"), eq(new BigDecimal("0"))))
            .thenReturn(Optional.empty());
        when(tierResolver.resolveTierForBalance(eq("t1"), eq("default"), eq(new BigDecimal("4000"))))
            .thenReturn(Optional.of(silver));
        when(tierDefinitionRepository.findByTenantIdAndProgrammeUidOrderByRankOrderAsc("t1", "default"))
            .thenReturn(List.of(bronze, silver));

        CustomerTierOutcome outcome = resolver.resolve(
            "t1", "default", "uuid-bronze", BigDecimal.ZERO, new BigDecimal("4000")
        );

        assertThat(outcome.tierBeforeUid()).isEqualTo("uuid-bronze");
        assertThat(outcome.tierAfterUid()).isEqualTo("uuid-silver");
        assertThat(outcome.tierBeforeName()).isEqualTo("Bronze");
        assertThat(outcome.tierAfterName()).isEqualTo("Silver");
        assertThat(outcome.tierChanged()).isTrue();
    }

    @Test
    void resolve_whenTierUnchanged_afterEarn() {
        TierDefinition silver = tier("uuid-silver", "Silver", 2);
        when(tierResolver.resolveTierForBalance(eq("t1"), eq("default"), eq(new BigDecimal("1000"))))
            .thenReturn(Optional.of(silver));
        when(tierResolver.resolveTierForBalance(eq("t1"), eq("default"), eq(new BigDecimal("1500"))))
            .thenReturn(Optional.of(silver));

        CustomerTierOutcome outcome = resolver.resolve(
            "t1", "default", "uuid-silver", new BigDecimal("1000"), new BigDecimal("1500")
        );

        assertThat(outcome.tierBeforeName()).isEqualTo("Silver");
        assertThat(outcome.tierAfterName()).isEqualTo("Silver");
        assertThat(outcome.tierChanged()).isFalse();
    }

    private static TierDefinition tier(String uid, String name, int rank) {
        TierDefinition tier = new TierDefinition();
        tier.setTierUid(uid);
        tier.setName(name);
        tier.setRankOrder(rank);
        tier.setEntryThreshold(BigDecimal.ZERO);
        return tier;
    }
}
