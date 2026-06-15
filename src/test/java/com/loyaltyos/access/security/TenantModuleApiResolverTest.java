package com.loyaltyos.access.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantModuleApiResolverTest {

    @ParameterizedTest
    @CsvSource({
        "/api/v1/me/dashboard, core_dashboard",
        "/api/v1/me/dashboard/overview, core_dashboard",
        "/api/v1/me/config, programme_config",
        "/api/v1/me/setup/rules/complete, loyalty_rules",
        "/api/v1/me/go-live/checklist, integrations",
        "/api/v1/me/integration/dashboard/overview, integrations",
        "/api/v1/me/merchants, merchants",
        "/api/v1/me/coupons/analytics, coupons",
        "/api/v1/me/referrals/programmes, referrals",
        "/api/v1/me/vouchers/batches, voucher_programs",
        "/api/v1/me/support/cases, support",
        "/api/v1/me/access/roles, team_admin",
        "/api/v1/campaigns/admin/campaigns, campaigns",
        "/api/v1/campaigns/admin/reports/summary, campaigns",
        "/api/v1/engine/rule/admin/rules, loyalty_rules",
        "/api/v1/engine/rule/evaluate, loyalty_rules",
        "/api/v2/programmes, programme_config",
        "/api/v2/programmes/p1/config, programme_config",
        "/api/v2/programmes/p1/config/event-schema/events, event_schema",
        "/api/v2/programmes/p1/reward-catalog, rewards_catalog",
        "/api/v2/programmes/p1/config/reward-catalog/restore, rewards_catalog",
        "/api/v1/analytics/enrollment, analytics_operational",
        "/api/v1/analytics/export/enrollment, analytics_operational",
        "/api/v1/analytics/reports/liability, analytics_finance",
        "/api/v1/analytics/cohort-analysis, analytics_cohort",
        "/api/v1/analytics/export/cohort, analytics_cohort"
    })
    void resolvesMappedPaths(String path, String expectedModule) {
        assertEquals(Optional.of(expectedModule), TenantModuleApiResolver.resolveModuleKey(path));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/v1/me/status",
        "/api/v1/me/profile",
        "/api/v1/me/identity",
        "/api/v1/me/agreement",
        "/api/v1/me/access",
        "/api/v1/auth/login",
        "/api/v1/onboarding/modules/catalog"
    })
    void leavesOnboardingAndAccessPathsUnguarded(String path) {
        assertTrue(TenantModuleApiResolver.resolveModuleKey(path).isEmpty());
    }

    @Test
    void returnsEmptyForUnknownPaths() {
        assertTrue(TenantModuleApiResolver.resolveModuleKey("/api/v1/unknown").isEmpty());
        assertTrue(TenantModuleApiResolver.resolveModuleKey(null).isEmpty());
        assertTrue(TenantModuleApiResolver.resolveModuleKey("").isEmpty());
    }
}
