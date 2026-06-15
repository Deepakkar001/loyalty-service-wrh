package com.loyaltyos.access.security;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Maps tenant portal API paths to access catalog module keys for entitlement filtering.
 * More specific rules are evaluated before broader prefix rules.
 */
public final class TenantModuleApiResolver {

    private TenantModuleApiResolver() {}

    private record PrefixRule(String prefix, String moduleKey) {}

    /** Onboarding / account paths that must stay reachable regardless of module entitlements. */
    private static final Set<String> UNGUARDED_EXACT_PATHS = Set.of(
        "/api/v1/me/status",
        "/api/v1/me/profile",
        "/api/v1/me/identity",
        "/api/v1/me/agreement"
    );

    private static final List<PrefixRule> PREFIX_RULES = List.of(
        new PrefixRule("/api/v1/me/integration", "integrations"),
        new PrefixRule("/api/v1/me/go-live", "integrations"),
        new PrefixRule("/api/v1/me/dashboard", "core_dashboard"),
        new PrefixRule("/api/v1/me/config", "programme_config"),
        new PrefixRule("/api/v1/me/setup", "loyalty_rules"),
        new PrefixRule("/api/v1/me/merchants", "merchants"),
        new PrefixRule("/api/v1/me/coupons", "coupons"),
        new PrefixRule("/api/v1/me/referrals", "referrals"),
        new PrefixRule("/api/v1/me/vouchers", "voucher_programs"),
        new PrefixRule("/api/v1/me/support", "support"),
        new PrefixRule("/api/v1/campaigns/admin", "campaigns"),
        new PrefixRule("/api/v1/engine/rule", "loyalty_rules"),
        new PrefixRule("/api/v1/analytics/export", "analytics_operational"),
        new PrefixRule("/api/v1/analytics", "analytics_operational"),
        new PrefixRule("/api/v1/rewards", "loyalty_rules")
    ).stream()
        .sorted(Comparator.comparingInt((PrefixRule r) -> r.prefix().length()).reversed())
        .toList();

    public static Optional<String> resolveModuleKey(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }

        if (UNGUARDED_EXACT_PATHS.contains(path)) {
            return Optional.empty();
        }

        if ("/api/v1/me/access".equals(path)) {
            return Optional.empty();
        }
        if (path.startsWith("/api/v1/me/access/")) {
            return Optional.of("team_admin");
        }

        Optional<String> programme = resolveProgrammeV2Module(path);
        if (programme.isPresent()) {
            return programme;
        }

        Optional<String> analytics = resolveAnalyticsModule(path);
        if (analytics.isPresent()) {
            return analytics;
        }

        for (PrefixRule rule : PREFIX_RULES) {
            if (matchesPrefix(path, rule.prefix())) {
                return Optional.of(rule.moduleKey());
            }
        }

        return Optional.empty();
    }

    private static boolean matchesPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static Optional<String> resolveProgrammeV2Module(String path) {
        if (!path.startsWith("/api/v2/programmes")) {
            return Optional.empty();
        }
        if (path.contains("/config/event-schema") || path.contains("/event-schema/")) {
            return Optional.of("event_schema");
        }
        if (path.contains("reward-catalog")) {
            return Optional.of("rewards_catalog");
        }
        return Optional.of("programme_config");
    }

    private static Optional<String> resolveAnalyticsModule(String path) {
        if (!path.startsWith("/api/v1/analytics")) {
            return Optional.empty();
        }
        if (path.contains("/cohorts/") || path.contains("/cohort-") || path.contains("/export/cohort")
            || path.contains("/segment-analysis") || path.contains("/export/segment")) {
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
        return Optional.of("analytics_operational");
    }
}
