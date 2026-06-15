package com.loyaltyos.access.service;

import java.util.Map;

/**
 * Human-readable module descriptions for catalog and admin entitlement UIs.
 */
public final class ModuleCatalogDescriptions {

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        Map.entry("core_dashboard", "Home dashboards, KPIs, and programme health overview."),
        Map.entry("programme_config", "Loyalty programme settings, tiers, and core configuration."),
        Map.entry("event_schema", "Define and manage inbound event schemas for integrations."),
        Map.entry("rewards_catalog", "Rewards catalogue setup, offers, and redemption configuration."),
        Map.entry("voucher_programs", "Voucher programmes, issuance rules, and redemption flows."),
        Map.entry("merchants", "Merchant onboarding, earn rates, campaigns, and partner management."),
        Map.entry("coupons", "Coupon creation, distribution, and redemption tracking."),
        Map.entry("loyalty_rules", "Accrual, redemption, and eligibility rule authoring."),
        Map.entry("campaigns", "Promotional campaigns, audiences, and offer orchestration."),
        Map.entry("referrals", "Referral programmes, milestones, and reward policies."),
        Map.entry("analytics_operational", "Operational analytics, enrolment, and SLA reporting."),
        Map.entry("analytics_finance", "Finance analytics including liability and breakage views."),
        Map.entry("analytics_cohort", "Cohort and segment analysis for advanced programmes."),
        Map.entry("integrations", "API keys, event ingestion, webhooks, and go-live integration."),
        Map.entry("team_admin", "Team members, roles, and fine-grained permissions."),
        Map.entry("billing", "Subscription plan, usage, and billing self-service."),
        Map.entry("support", "Support cases, documentation, and platform assistance.")
    );

    private ModuleCatalogDescriptions() {}

    public static String forModule(String moduleKey, String displayNameFallback) {
        return DESCRIPTIONS.getOrDefault(moduleKey, displayNameFallback);
    }
}
