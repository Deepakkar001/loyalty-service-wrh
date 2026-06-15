package com.loyaltyos.access.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.access.entity.AccessAction;
import com.loyaltyos.access.entity.AccessModule;
import com.loyaltyos.access.entity.AccessModuleAction;
import com.loyaltyos.access.entity.AccessNavItem;
import com.loyaltyos.access.entity.TenantRoleTemplate;
import com.loyaltyos.access.entity.TierModuleBaseline;
import com.loyaltyos.access.repository.AccessActionRepository;
import com.loyaltyos.access.repository.AccessModuleActionRepository;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.AccessNavItemRepository;
import com.loyaltyos.access.repository.TenantRoleTemplateRepository;
import com.loyaltyos.access.repository.TierModuleBaselineRepository;
import com.loyaltyos.onboarding.enums.SubscriptionTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(10)
public class AccessCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccessCatalogSeeder.class);

    private final AccessModuleRepository moduleRepository;
    private final AccessActionRepository actionRepository;
    private final AccessModuleActionRepository moduleActionRepository;
    private final AccessNavItemRepository navItemRepository;
    private final TierModuleBaselineRepository tierBaselineRepository;
    private final TenantRoleTemplateRepository roleTemplateRepository;
    private final ObjectMapper objectMapper;

    public AccessCatalogSeeder(
        AccessModuleRepository moduleRepository,
        AccessActionRepository actionRepository,
        AccessModuleActionRepository moduleActionRepository,
        AccessNavItemRepository navItemRepository,
        TierModuleBaselineRepository tierBaselineRepository,
        TenantRoleTemplateRepository roleTemplateRepository,
        ObjectMapper objectMapper
    ) {
        this.moduleRepository = moduleRepository;
        this.actionRepository = actionRepository;
        this.moduleActionRepository = moduleActionRepository;
        this.navItemRepository = navItemRepository;
        this.tierBaselineRepository = tierBaselineRepository;
        this.roleTemplateRepository = roleTemplateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (moduleRepository.count() > 0) {
            return;
        }
        log.info("Seeding access control catalog...");
        seedActions();
        seedModules();
        seedModuleActions();
        seedNavItems();
        seedTierBaselines();
        seedRoleTemplates();
        log.info("Access control catalog seeded.");
    }

    private void seedActions() {
        String[][] actions = {
            {"view", "View", "1"},
            {"create", "Create", "2"},
            {"edit", "Edit", "3"},
            {"delete", "Delete", "4"},
            {"publish", "Publish", "5"},
            {"export", "Export", "6"},
            {"approve", "Approve", "7"},
        };
        for (String[] row : actions) {
            AccessAction a = new AccessAction();
            a.setActionKey(row[0]);
            a.setDisplayName(row[1]);
            a.setSortOrder(Integer.parseInt(row[2]));
            actionRepository.save(a);
        }
    }

    private void seedModules() {
        Object[][] modules = {
            {"core_dashboard", "Dashboard", "Dashboard", 10, "LayoutGrid", true, null},
            {"programme_config", "Programme Configuration", "Configuration", 20, "Settings", true, null},
            {"event_schema", "Event Schema", "Configuration", 30, "DatabaseZap", true, null},
            {"rewards_catalog", "Rewards Catalog", "Configuration", 40, "Star", false, null},
            {"voucher_programs", "Voucher Programs", "Configuration", 50, "Star", false, null},
            {"merchants", "Merchants", "Configuration", 60, "Users", false, null},
            {"coupons", "Coupons", "Configuration", 70, "TicketPercent", false, null},
            {"loyalty_rules", "Loyalty Rules", "Loyalty Rules", 80, "GitBranchPlus", true, null},
            {"campaigns", "Campaigns", "Campaigns", 90, "Megaphone", false, null},
            {"referrals", "Referrals", "Referrals", 100, "Users", false, null},
            {"analytics_operational", "Operational Analytics", "Analytics & Reports", 110, "BarChart3", true, null},
            {"analytics_finance", "Finance Analytics", "Analytics & Reports", 120, "HandCoins", false, SubscriptionTier.PROFESSIONAL},
            {"analytics_cohort", "Cohort Analytics", "Analytics & Reports", 130, "Layers", false, SubscriptionTier.ENTERPRISE},
            {"integrations", "Integrations", "Settings", 140, "Plug", true, null},
            {"team_admin", "Team & Permissions", "Settings", 150, "Users", true, null},
            {"billing", "Billing & Plan", "Settings", 160, "HandCoins", false, null},
            {"support", "Support", "Support", 170, "Headset", true, null},
        };
        for (Object[] row : modules) {
            AccessModule m = new AccessModule();
            m.setModuleKey((String) row[0]);
            m.setDisplayName((String) row[1]);
            m.setNavSection((String) row[2]);
            m.setSortOrder((Integer) row[3]);
            m.setIconKey((String) row[4]);
            m.setRequired((Boolean) row[5]);
            m.setMinSubscriptionTier((SubscriptionTier) row[6]);
            m.setActive(true);
            moduleRepository.save(m);
        }
    }

    private void seedModuleActions() {
        String[] modules = {
            "core_dashboard", "programme_config", "event_schema", "rewards_catalog", "voucher_programs",
            "merchants", "coupons", "loyalty_rules", "campaigns", "referrals",
            "analytics_operational", "analytics_finance", "analytics_cohort",
            "integrations", "team_admin", "billing", "support"
        };
        String[] standardActions = {"view", "create", "edit", "delete", "export"};
        String[] publishModules = {"campaigns", "loyalty_rules"};
        String[] approveModules = {"merchants", "referrals"};

        for (String module : modules) {
            Set<String> actions = new LinkedHashSet<>(List.of(standardActions));
            if (List.of(publishModules).contains(module)) {
                actions.add("publish");
            }
            if (List.of(approveModules).contains(module)) {
                actions.add("approve");
            }
            if (module.startsWith("analytics") || module.equals("support")) {
                actions.remove("create");
                actions.remove("delete");
                actions.remove("publish");
            }
            if (module.equals("core_dashboard")) {
                actions.remove("create");
                actions.remove("edit");
                actions.remove("delete");
                actions.remove("export");
            }
            if (module.equals("billing")) {
                actions.retainAll(Set.of("view"));
            }
            for (String action : actions) {
                AccessModuleAction ma = new AccessModuleAction();
                ma.setModuleKey(module);
                ma.setActionKey(action);
                ma.setPermissionKey(module + "." + action);
                ma.setAssignable(true);
                moduleActionRepository.save(ma);
            }
        }
    }

    private void seedNavItems() {
        List<NavSeed> items = new ArrayList<>();
        items.add(new NavSeed("core_dashboard", "/dashboard", "Overview", 1, "core_dashboard.view", true, "LayoutGrid"));
        items.add(new NavSeed("programme_config", "/dashboard/configure", "Configure Programme", 10, "programme_config.view", false, "Settings"));
        items.add(new NavSeed("programme_config", "/dashboard/configure/my-configurations", "My Configurations", 11, "programme_config.view", true, "Search"));
        items.add(new NavSeed("event_schema", "/dashboard/setup/event-schema", "Event Schema", 20, "event_schema.view", true, "DatabaseZap"));
        items.add(new NavSeed("rewards_catalog", "/dashboard/setup/rewards-catalog", "Rewards Catalog", 30, "rewards_catalog.view", true, "Star"));
        items.add(new NavSeed("voucher_programs", "/dashboard/setup/voucher-programs", "Voucher Programs", 40, "voucher_programs.view", true, "Star"));
        items.add(new NavSeed("merchants", "/dashboard/configure/merchants", "Merchants", 50, "merchants.view", true, "Users"));
        items.add(new NavSeed("coupons", "/dashboard/coupons", "Coupons", 60, "coupons.view", true, "TicketPercent"));
        items.add(new NavSeed("coupons", "/dashboard/coupons/analytics", "Coupon Analytics", 61, "coupons.view", true, "BarChart3"));
        items.add(new NavSeed("loyalty_rules", "/dashboard/loyalty-rules/create/basic-info", "Create Rule", 70, "loyalty_rules.view", false, "GitBranchPlus"));
        items.add(new NavSeed("loyalty_rules", "/dashboard/loyalty-rules/my-rules", "My Rules", 71, "loyalty_rules.view", false, "Search"));
        items.add(new NavSeed("campaigns", "/dashboard/campaigns", "Campaigns", 80, "campaigns.view", true, "Megaphone"));
        items.add(new NavSeed("campaigns", "/dashboard/campaigns/create", "Create Campaign", 81, "campaigns.create", true, "GitBranchPlus"));
        items.add(new NavSeed("campaigns", "/dashboard/campaign-rules/create/campaign?new=1", "Create Campaign Rule", 82, "campaigns.create", true, "GitBranchPlus"));
        items.add(new NavSeed("campaigns", "/dashboard/campaigns/reports", "Campaign Reports", 83, "campaigns.export", true, "Download"));
        items.add(new NavSeed("referrals", "/dashboard/referrals/my-referrals", "My Referrals", 90, "referrals.view", true, "Users"));
        items.add(new NavSeed("referrals", "/dashboard/referrals/create", "Create Referral", 91, "referrals.create", true, "GitBranchPlus"));
        items.add(new NavSeed("referrals", "/dashboard/referrals/analytics", "Referral Analytics", 92, "referrals.view", true, "BarChart3"));
        items.add(new NavSeed("referrals", "/dashboard/referrals/fraud-review", "Fraud Review", 93, "referrals.approve", true, "ShieldCheck"));
        items.add(new NavSeed("analytics_operational", "/dashboard/analytics/custom-reports", "Custom Reports", 100, "analytics_operational.view", true, "BarChart3"));
        items.add(new NavSeed("analytics_operational", "/dashboard/analytics/enrollment", "Enrollment", 101, "analytics_operational.view", true, "UserPlus"));
        items.add(new NavSeed("analytics_operational", "/dashboard/analytics/failed-accruals-redemptions", "Failed Accruals & Redemptions", 102, "analytics_operational.view", true, "AlertTriangle"));
        items.add(new NavSeed("analytics_operational", "/dashboard/analytics/sla-performance", "SLA & Performance", 103, "analytics_operational.view", true, "Gauge"));
        items.add(new NavSeed("analytics_operational", "/dashboard/analytics/export-data", "Export Data", 104, "analytics_operational.export", true, "Download"));
        items.add(new NavSeed("analytics_finance", "/dashboard/analytics/breakage-expiry", "Breakage & Expiry", 110, "analytics_finance.view", true, "Timer"));
        items.add(new NavSeed("analytics_finance", "/dashboard/analytics/accrual-redemption-reconciliation", "Accrual & Reconciliation", 111, "analytics_finance.view", true, "Scale"));
        items.add(new NavSeed("analytics_finance", "/dashboard/analytics/liability", "Liability Report", 112, "analytics_finance.view", true, "HandCoins"));
        items.add(new NavSeed("analytics_finance", "/dashboard/analytics/reversals-adjustments", "Reversals & Adjustments", 113, "analytics_finance.view", true, "RotateCcw"));
        items.add(new NavSeed("analytics_cohort", "/dashboard/analytics/segment-analysis", "Segment Analysis", 120, "analytics_cohort.view", true, "Users"));
        items.add(new NavSeed("analytics_cohort", "/dashboard/analytics/cohort-analysis", "Cohort Analysis", 121, "analytics_cohort.view", true, "Layers"));
        items.add(new NavSeed("integrations", "/dashboard/integration", "Integrations", 130, "integrations.view", false, "Plug"));
        items.add(new NavSeed("team_admin", "/dashboard/settings/team", "Team & Permissions", 140, "team_admin.view", true, "Users"));
        items.add(new NavSeed("billing", "/dashboard/settings/billing", "Billing & Plan", 141, "billing.view", true, "HandCoins"));
        items.add(new NavSeed("support", "/dashboard/support/docs", "Documentation", 150, "support.view", true, "BookOpenText"));
        items.add(new NavSeed("support", "/dashboard/support/contact", "Contact Support", 151, "support.view", true, "Headset"));
        items.add(new NavSeed("support", "/dashboard/support/community", "Community Forum", 152, "support.view", true, "CircleHelp"));
        items.add(new NavSeed("programme_config", "/dashboard/profile", "Your Profile", 5, "programme_config.view", true, "User"));

        for (NavSeed seed : items) {
            AccessNavItem nav = new AccessNavItem();
            nav.setModuleKey(seed.moduleKey);
            nav.setRoutePath(seed.routePath);
            nav.setLabelKey(seed.labelKey);
            nav.setSortOrder(seed.sortOrder);
            nav.setRequiredPermissionKey(seed.requiredPermission);
            nav.setRequiresOnboardingComplete(seed.requiresOnboardingComplete);
            nav.setIconKey(seed.iconKey);
            navItemRepository.save(nav);
        }
    }

    private void seedTierBaselines() {
        seedTier(SubscriptionTier.STANDARD, List.of(
            "rewards_catalog", "voucher_programs", "analytics_operational"
        ), List.of());
        seedTier(SubscriptionTier.PROFESSIONAL, List.of(
            "rewards_catalog", "voucher_programs", "analytics_operational",
            "analytics_finance", "campaigns", "coupons"
        ), List.of("rewards_catalog", "voucher_programs", "analytics_operational", "campaigns", "coupons"));
        seedTier(SubscriptionTier.ENTERPRISE, List.of(
            "rewards_catalog", "voucher_programs", "analytics_operational",
            "analytics_finance", "analytics_cohort", "campaigns", "coupons",
            "referrals", "merchants"
        ), List.of(
            "rewards_catalog", "voucher_programs", "analytics_operational",
            "analytics_finance", "campaigns", "coupons", "referrals", "merchants"
        ));
    }

    private void seedTier(SubscriptionTier tier, List<String> modules, List<String> locked) {
        for (String moduleKey : modules) {
            TierModuleBaseline baseline = new TierModuleBaseline(tier, moduleKey, locked.contains(moduleKey));
            tierBaselineRepository.save(baseline);
        }
    }

    private void seedRoleTemplates() {
        saveTemplate("programme_manager", "Programme Manager",
            "Manage programme, rules, and event schema",
            List.of("programme_config.view", "programme_config.edit", "loyalty_rules.view", "loyalty_rules.create",
                "loyalty_rules.edit", "event_schema.view", "event_schema.edit", "analytics_operational.view"));
        saveTemplate("campaign_manager", "Campaign Manager",
            "Manage campaigns and related reports",
            List.of("campaigns.view", "campaigns.create", "campaigns.edit", "campaigns.publish", "campaigns.export",
                "analytics_operational.view"));
        saveTemplate("finance_viewer", "Finance Viewer",
            "View finance analytics and exports",
            List.of("analytics_finance.view", "analytics_finance.export", "analytics_operational.export"));
        saveTemplate("integration_engineer", "Integration Engineer",
            "Manage API keys and integration settings",
            List.of("integrations.view", "integrations.edit", "integrations.create"));
        saveTemplate("support_agent", "Support Agent",
            "Access support resources",
            List.of("support.view"));
    }

    private void saveTemplate(String key, String name, String description, List<String> permissions) {
        TenantRoleTemplate t = new TenantRoleTemplate();
        t.setTemplateKey(key);
        t.setRoleName(name);
        t.setDescription(description);
        try {
            t.setPermissionKeys(objectMapper.writeValueAsString(permissions));
        } catch (JsonProcessingException e) {
            t.setPermissionKeys("[]");
        }
        roleTemplateRepository.save(t);
    }

    private record NavSeed(
        String moduleKey, String routePath, String labelKey, int sortOrder,
        String requiredPermission, boolean requiresOnboardingComplete, String iconKey
    ) {}
}
