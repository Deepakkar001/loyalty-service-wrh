package com.loyaltyos.access.service;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.enums.EntitlementSource;
import com.loyaltyos.access.repository.AccessNavItemRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.ContactRole;
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

    public AccessMigrationService(
        AccessProperties accessProperties,
        TenantOnboardingRepository tenantRepository,
        TenantContactRepository contactRepository,
        TenantUserRepository userRepository,
        AccessProvisioningService provisioningService,
        AccessNavItemRepository navItemRepository
    ) {
        this.accessProperties = accessProperties;
        this.tenantRepository = tenantRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
        this.navItemRepository = navItemRepository;
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
}
