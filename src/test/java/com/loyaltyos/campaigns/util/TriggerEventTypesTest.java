package com.loyaltyos.campaigns.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TriggerEventTypesTest {

    @Test
    void parse_splitsCommaSeparatedTypes() {
        assertThat(TriggerEventTypes.parse("purchase, REFUND,PURCHASE"))
            .containsExactly("PURCHASE", "REFUND");
    }

    @Test
    void normalize_dedupesAndUppercases() {
        assertThat(TriggerEventTypes.normalize(" purchase ,refund,purchase "))
            .isEqualTo("PURCHASE,REFUND");
    }

    @Test
    void contains_matchesAnyStoredType() {
        assertThat(TriggerEventTypes.contains("PURCHASE,REFUND", "refund")).isTrue();
        assertThat(TriggerEventTypes.contains("PURCHASE", "ORDER")).isFalse();
        assertThat(TriggerEventTypes.contains("LOGIN,PURCHASE", "LOGIN")).isTrue();
        assertThat(TriggerEventTypes.contains("LOGIN,PURCHASE", "LOGIN,PURCHASE")).isFalse();
    }

    @Test
    void resolveSingleForCampaignRule_acceptsOneTypeFromMultiValueCampaign() {
        assertThat(TriggerEventTypes.resolveSingleForCampaignRule("LOGIN,PURCHASE", "LOGIN"))
            .isEqualTo("LOGIN");
    }

    @Test
    void resolveSingleForCampaignRule_rejectsCommaSeparatedCandidate() {
        assertThatThrownBy(() -> TriggerEventTypes.resolveSingleForCampaignRule("LOGIN,PURCHASE", "LOGIN,PURCHASE"))
            .hasMessageContaining("single event type");
    }

    @Test
    void resolveSingleForCampaignRule_acceptsTypeWhenCampaignHasNoneYet() {
        assertThat(TriggerEventTypes.resolveSingleForCampaignRule("", "PURCHASE"))
            .isEqualTo("PURCHASE");
        assertThat(TriggerEventTypes.resolveSingleForCampaignRule(null, "login"))
            .isEqualTo("LOGIN");
    }

    @Test
    void validateSerialized_requiresAtLeastOneType() {
        assertThatThrownBy(() -> TriggerEventTypes.validateSerialized("  "))
            .hasMessageContaining("required");
    }
}
