package com.loyaltyos.access.service;

import com.loyaltyos.access.dto.SaveModulesRequest;
import com.loyaltyos.access.entity.AccessModule;
import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.enums.EntitlementSource;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.TenantModuleEntitlementRepository;
import com.loyaltyos.access.repository.TierModuleBaselineRepository;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.SubscriptionTier;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantEntitlementServiceTest {

    @Mock private TenantOnboardingRepository tenantRepository;
    @Mock private AccessModuleRepository moduleRepository;
    @Mock private TierModuleBaselineRepository tierBaselineRepository;
    @Mock private TenantModuleEntitlementRepository entitlementRepository;
    @Mock private AccessProvisioningService provisioningService;
    @Mock private IntegrationApiKeyLifecycleService integrationApiKeyLifecycleService;
    @Mock private AccessControlAuditService auditService;

    private TenantEntitlementService service;

    @BeforeEach
    void setUp() {
        service = new TenantEntitlementService(
            tenantRepository,
            moduleRepository,
            tierBaselineRepository,
            entitlementRepository,
            provisioningService,
            integrationApiKeyLifecycleService,
            auditService
        );
    }

    @Test
    void updateAdminModules_honorsExplicitEnabledListWithoutForcingRequired() {
        String tenantId = "tenant-1";
        AccessModule required = module("core_dashboard", true);
        AccessModule optional = module("campaigns", false);

        when(moduleRepository.findByActiveTrueOrderBySortOrderAsc())
            .thenReturn(List.of(required, optional));
        when(entitlementRepository.findById(any())).thenReturn(Optional.empty());
        when(entitlementRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of());

        service.updateAdminModules(tenantId, List.of("campaigns"), "admin-1");

        ArgumentCaptor<TenantModuleEntitlement> captor = ArgumentCaptor.forClass(TenantModuleEntitlement.class);
        verify(entitlementRepository, atLeastOnce()).save(captor.capture());

        TenantModuleEntitlement savedDashboard = captor.getAllValues().stream()
            .filter(e -> "core_dashboard".equals(e.getModuleKey()))
            .findFirst()
            .orElseThrow();
        assertFalse(savedDashboard.isEnabled(), "Admin must be able to disable required modules");

        TenantModuleEntitlement savedCampaigns = captor.getAllValues().stream()
            .filter(e -> "campaigns".equals(e.getModuleKey()))
            .findFirst()
            .orElseThrow();
        assertTrue(savedCampaigns.isEnabled());
        assertEquals(EntitlementSource.PLATFORM_ADMIN, savedCampaigns.getSource());
    }

    @Test
    void saveOnboardingModules_persistsSelectedOptionalModules() {
        String tenantId = "tenant-1";
        TenantOnboarding tenant = new TenantOnboarding();
        tenant.setTenantId(tenantId);
        tenant.setSubscriptionTier(SubscriptionTier.STANDARD);

        AccessModule required = module("integrations", true);
        AccessModule optional = module("referrals", false);

        when(tenantRepository.findByTenantId(tenantId)).thenReturn(Optional.of(tenant));
        when(moduleRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(required, optional));
        when(tierBaselineRepository.findBySubscriptionTier(SubscriptionTier.STANDARD)).thenReturn(List.of());
        when(entitlementRepository.findByTenantId(tenantId)).thenReturn(List.of());

        SaveModulesRequest request = new SaveModulesRequest();
        request.setSelectedModuleKeys(List.of("referrals"));

        service.saveOnboardingModules(tenantId, request);
        ArgumentCaptor<TenantModuleEntitlement> captor = ArgumentCaptor.forClass(TenantModuleEntitlement.class);
        verify(entitlementRepository, atLeastOnce()).save(captor.capture());

        assertTrue(captor.getAllValues().stream().anyMatch(e ->
            "integrations".equals(e.getModuleKey()) && e.isEnabled()));
        assertTrue(captor.getAllValues().stream().anyMatch(e ->
            "referrals".equals(e.getModuleKey()) && e.isEnabled()));
        assertEquals(EntitlementSource.ONBOARDING, captor.getAllValues().get(0).getSource());
    }

    private static AccessModule module(String key, boolean required) {
        AccessModule module = new AccessModule();
        module.setModuleKey(key);
        module.setDisplayName(key);
        module.setRequired(required);
        module.setActive(true);
        return module;
    }
}
