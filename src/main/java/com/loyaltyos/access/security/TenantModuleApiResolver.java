package com.loyaltyos.access.security;

import java.util.List;
import java.util.Optional;

/**
 * Maps tenant portal API paths to access catalog module keys for entitlement filtering.
 */
public final class TenantModuleApiResolver {

    private TenantModuleApiResolver() {}

    private record PrefixRule(String prefix, String moduleKey) {}

    private static final List<PrefixRule> PREFIX_RULES = List.of(
        new PrefixRule("/api/v1/me/integration", "integrations"),
        new PrefixRule("/api/v1/me/merchants", "merchants"),
        new PrefixRule("/api/v1/me/coupons", "coupons"),
        new PrefixRule("/api/v1/campaigns/admin", "campaigns"),
        new PrefixRule("/api/v1/me/referrals", "referrals"),
        new PrefixRule("/api/v1/engine/rule/admin", "loyalty_rules"),
        new PrefixRule("/api/v1/me/vouchers", "voucher_programs"),
        new PrefixRule("/api/v1/analytics/export", "analytics_operational"),
        new PrefixRule("/api/v1/analytics", "analytics_operational"),
        new PrefixRule("/api/v1/me/support", "support")
    );

    public static Optional<String> resolveModuleKey(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        if ("/api/v1/me/access".equals(path)) {
            return Optional.empty();
        }
        if (path.startsWith("/api/v1/me/access/")) {
            return Optional.of("team_admin");
        }
        Optional<String> analytics = resolveAnalyticsModule(path);
        if (analytics.isPresent()) {
            return analytics;
        }
        for (PrefixRule rule : PREFIX_RULES) {
            if (path.equals(rule.prefix()) || path.startsWith(rule.prefix() + "/")) {
                return Optional.of(rule.moduleKey());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> resolveAnalyticsModule(String path) {
        if (!path.startsWith("/api/v1/analytics")) {
            return Optional.empty();
        }
        if (path.contains("/cohorts/") || path.contains("/cohort-") || path.contains("/export/cohort")) {
            return Optional.of("analytics_cohort");
        }
        if (path.contains("/reports/liability")
            || path.contains("/reports/breakage-expiry")
            || path.contains("/export/liability")
            || path.contains("/export/breakage")) {
            return Optional.of("analytics_finance");
        }
        if (path.startsWith("/api/v1/analytics/export")) {
            return Optional.of("analytics_operational");
        }
        if (path.startsWith("/api/v1/analytics")) {
            return Optional.of("analytics_operational");
        }
        return Optional.empty();
    }
}
