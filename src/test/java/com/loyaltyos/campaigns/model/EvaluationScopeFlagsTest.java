package com.loyaltyos.campaigns.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvaluationScopeFlagsTest {

    @Test
    void blank_defaultsToCampaignsAndRulesNotReferral() {
        EvaluationScopeFlags f = EvaluationScopeFlags.parse(null);
        assertThat(f.runCampaigns()).isTrue();
        assertThat(f.runProgrammeRules()).isTrue();
        assertThat(f.runReferral()).isFalse();
    }

    @Test
    void referralOnly() {
        EvaluationScopeFlags f = EvaluationScopeFlags.parse("REFERRAL");
        assertThat(f.runCampaigns()).isFalse();
        assertThat(f.runProgrammeRules()).isFalse();
        assertThat(f.runReferral()).isTrue();
    }

    @Test
    void combinedReferralAndRules() {
        EvaluationScopeFlags f = EvaluationScopeFlags.parse("REFERRAL,PROGRAMME_RULES");
        assertThat(f.runCampaigns()).isFalse();
        assertThat(f.runProgrammeRules()).isTrue();
        assertThat(f.runReferral()).isTrue();
    }
}
