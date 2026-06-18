package com.loyaltyos.access.service;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.entity.AccessNavItem;
import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.enums.EntitlementSource;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.AccessNavItemRepository;
import com.loyaltyos.access.repository.TenantModuleEntitlementRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.ContactRole;
import com.loyaltyos.onboarding.enums.SubscriptionTier;
import com.loyaltyos.onboarding.repository.TenantContactRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class AccessMigrationService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccessMigrationService.class);

    private final AccessProperties accessProperties;
    private final TenantOnboardingRepository tenantRepository;
    private final TenantContactRepository contactRepository;
    private final TenantUserRepository userRepository;
    private final AccessProvisioningService provisioningService;
    private final AccessNavItemRepository navItemRepository;
    private final AccessModuleRepository moduleRepository;
    private final TenantModuleEntitlementRepository entitlementRepository;

    public AccessMigrationService(
        AccessProperties accessProperties,
        TenantOnboardingRepository tenantRepository,
        TenantContactRepository contactRepository,
        TenantUserRepository userRepository,
        AccessProvisioningService provisioningService,
        AccessNavItemRepository navItemRepository,
        AccessModuleRepository moduleRepository,
        TenantModuleEntitlementRepository entitlementRepository
    ) {
        this.accessProperties = accessProperties;
        this.tenantRepository = tenantRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
        this.navItemRepository = navItemRepository;
        this.moduleRepository = moduleRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!accessProperties.getMigration().isRunOnStartup()) {
            return;
        }
        patchNavCatalog();
        tenantRepository.findAll().forEach(this::migrateTenantSafe);
    }

    @Transactional
    protected void patchNavCatalog() {
        navItemRepository.findAllByOrderBySortOrderAsc().stream()
            .filter(item -> "/dashboard/loyalty-rules/my-rules".equals(item.getRoutePath()))
            .forEach(item -> {
                if (item.isRequiresOnboardingComplete()) {
                    item.setRequiresOnboardingComplete(false);
                    navItemRepository.save(item);
                    log.info("Updated My Rules nav item to be available during setup progress");
                }
            });
        ensureMerchantsNavItem();
    }

    private void ensureMerchantsNavItem() {
        boolean hasMerchantsNav = navItemRepository.findAllByOrderBySortOrderAsc().stream()
            .anyMatch(item -> "merchants".equals(item.getModuleKey())
                && "/dashboard/configure/merchants".equals(item.getRoutePath()));
        if (hasMerchantsNav) {
            return;
        }
        if (moduleRepository.findById("merchants").isEmpty()) {
            return;
        }
        AccessNavItem nav = new AccessNavItem();
        nav.setModuleKey("merchants");
        nav.setRoutePath("/dashboard/configure/merchants");
        nav.setLabelKey("Merchants");
        nav.setSortOrder(50);
        nav.setRequiredPermissionKey("merchants.view");
        nav.setRequiresOnboardingComplete(true);
        nav.setIconKey("Users");
        navItemRepository.save(nav);
        log.info("Patched access catalog with merchants nav item");
    }

    private void migrateTenantSafe(TenantOnboarding tenant) {
        try {
            migrateTenant(tenant);
        } catch (Exception e) {
            log.warn("Access migration failed for tenant {}: {}", tenant.getTenantId(), e.getMessage());
        }
    }

    @Transactional
    protected void migrateTenant(TenantOnboarding tenant) {
        String tenantId = tenant.getTenantId();
        if (!Boolean.TRUE.equals(tenant.getEmailVerified())) {
            return;
        }

        if (!provisioningService.hasEntitlements(tenantId)) {
            provisioningService.provisionFullEntitlements(tenantId, EntitlementSource.MIGRATION, "system");
        } else {
            ensureMerchantsEntitlementForEnterprise(tenant);
        }

        if (!userRepository.existsByTenantId(tenantId)) {
            String fullName = contactRepository.findByTenantIdAndRole(tenantId, ContactRole.PRIMARY_ADMIN)
                .map(c -> c.getName())
                .orElse(null);
            var user = provisioningService.provisionPrimaryAdmin(
                tenantId, tenant.getEmail(), tenant.getPasswordHash(), fullName);
            var role = provisioningService.provisionSystemRole(tenantId);
            provisioningService.grantAllEntitledPermissions(tenantId, role.getRoleId());
            log.info("Migrated tenant {} to tenant_user {}", tenantId, user.getUserId());
        } else {
            provisioningService.syncPrimaryAdminGrants(tenantId);
        }
    }

    /**
     * Tenants onboarded before the merchants module shipped may lack the entitlement even on Enterprise tier.
     * Add it without removing other module selections.
     */
    private void ensureMerchantsEntitlementForEnterprise(TenantOnboarding tenant) {
        if (tenant.getSubscriptionTier() != SubscriptionTier.ENTERPRISE
            && tenant.getSubscriptionTier() != SubscriptionTier.PROFESSIONAL) {
            return;
        }
        String tenantId = tenant.getTenantId();
        boolean hasMerchants = entitlementRepository.findByTenantId(tenantId).stream()
            .anyMatch(e -> "merchants".equals(e.getModuleKey()) && e.isEnabled());
        if (hasMerchants) {
            return;
        }
        if (moduleRepository.findById("merchants").isEmpty()) {
            return;
        }
        TenantModuleEntitlement ent = new TenantModuleEntitlement();
        ent.setTenantId(tenantId);
        ent.setModuleKey("merchants");
        ent.setEnabled(true);
        ent.setSource(EntitlementSource.MIGRATION);
        ent.setEnabledBy("system");
        entitlementRepository.save(ent);
        provisioningService.syncPrimaryAdminGrants(tenantId);
        log.info("Enabled merchants module entitlement for tenant {}", tenantId);
    }
}
