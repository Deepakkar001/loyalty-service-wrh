package com.loyaltyos.access.service;

import com.loyaltyos.access.dto.ModuleCatalogResponse;
import com.loyaltyos.access.dto.ModuleCatalogItemDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TenantEntitlementService {

    private final TenantOnboardingRepository tenantRepository;
    private final AccessModuleRepository moduleRepository;
    private final TierModuleBaselineRepository tierBaselineRepository;
    private final TenantModuleEntitlementRepository entitlementRepository;
    private final AccessProvisioningService provisioningService;
    private final IntegrationApiKeyLifecycleService integrationApiKeyLifecycleService;
    private final AccessControlAuditService auditService;

    public TenantEntitlementService(
        TenantOnboardingRepository tenantRepository,
        AccessModuleRepository moduleRepository,
        TierModuleBaselineRepository tierBaselineRepository,
        TenantModuleEntitlementRepository entitlementRepository,
        AccessProvisioningService provisioningService,
        IntegrationApiKeyLifecycleService integrationApiKeyLifecycleService,
        AccessControlAuditService auditService
    ) {
        this.tenantRepository = tenantRepository;
        this.moduleRepository = moduleRepository;
        this.tierBaselineRepository = tierBaselineRepository;
        this.entitlementRepository = entitlementRepository;
        this.provisioningService = provisioningService;
        this.integrationApiKeyLifecycleService = integrationApiKeyLifecycleService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ModuleCatalogResponse getCatalog(String tenantId) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        SubscriptionTier tier = tenant.getSubscriptionTier();

        List<AccessModule> modules = moduleRepository.findByActiveTrueOrderBySortOrderAsc();
        Set<String> tierBaseline = tierBaselineRepository.findBySubscriptionTier(tier).stream()
            .map(b -> b.getModuleKey())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> tierLocked = tierBaselineRepository.findBySubscriptionTier(tier).stream()
            .filter(b -> b.isLocked())
            .map(b -> b.getModuleKey())
            .collect(Collectors.toSet());

        List<String> required = modules.stream().filter(AccessModule::isRequired).map(AccessModule::getModuleKey).toList();

        List<ModuleCatalogItemDto> items = modules.stream().map(m -> {
            ModuleCatalogItemDto dto = new ModuleCatalogItemDto();
            dto.setModuleKey(m.getModuleKey());
            dto.setDisplayName(m.getDisplayName());
            dto.setDescription(ModuleCatalogDescriptions.forModule(m.getModuleKey(), m.getDisplayName()));
            dto.setRequired(m.isRequired());
            dto.setInTierBaseline(tierBaseline.contains(m.getModuleKey()));
            dto.setLocked(m.isRequired() || tierLocked.contains(m.getModuleKey()));
            dto.setPreSelected(m.isRequired() || tierBaseline.contains(m.getModuleKey()));
            return dto;
        }).toList();

        ModuleCatalogResponse response = new ModuleCatalogResponse();
        response.setTier(tier.name());
        response.setRequired(required);
        response.setTierBaseline(List.copyOf(tierBaseline));
        response.setModules(items);
        response.setModulesConfigured(entitlementRepository.existsByTenantId(tenantId));
        return response;
    }

    @Transactional
    public void saveOnboardingModules(String tenantId, SaveModulesRequest request) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        SubscriptionTier tier = tenant.getSubscriptionTier();

        Set<String> effective = computeEffectiveModules(tier, request.getSelectedModuleKeys());
        entitlementRepository.findByTenantId(tenantId).forEach(entitlementRepository::delete);

        for (String moduleKey : effective) {
            TenantModuleEntitlement ent = new TenantModuleEntitlement();
            ent.setTenantId(tenantId);
            ent.setModuleKey(moduleKey);
            ent.setEnabled(true);
            ent.setSource(EntitlementSource.ONBOARDING);
            ent.setEnabledBy(tenantId);
            entitlementRepository.save(ent);
        }
        provisioningService.syncPrimaryAdminGrants(tenantId);
        auditService.log(tenantId, "TENANT", tenantId, "MODULES_ONBOARDING_SAVED",
            Map.of("moduleKeys", effective));
    }

    @Transactional
    public void updateAdminModules(String tenantId, List<String> enabledModuleKeys, String adminUid) {
        boolean integrationsWasEnabled = isModuleEnabled(tenantId, "integrations");
        Set<String> enabled = new HashSet<>(enabledModuleKeys);
        List<AccessModule> allModules = moduleRepository.findByActiveTrueOrderBySortOrderAsc();

        for (AccessModule module : allModules) {
            TenantModuleEntitlement ent = entitlementRepository
                .findById(new com.loyaltyos.access.entity.TenantModuleEntitlementId(tenantId, module.getModuleKey()))
                .orElseGet(() -> {
                    TenantModuleEntitlement e = new TenantModuleEntitlement();
                    e.setTenantId(tenantId);
                    e.setModuleKey(module.getModuleKey());
                    return e;
                });
            boolean shouldEnable = enabled.contains(module.getModuleKey());
            ent.setEnabled(shouldEnable);
            ent.setSource(EntitlementSource.PLATFORM_ADMIN);
            ent.setEnabledBy(adminUid);
            entitlementRepository.save(ent);
        }
        provisioningService.syncPrimaryAdminGrants(tenantId);
        bumpSessionVersionForTenant(tenantId);

        boolean integrationsNowEnabled = isModuleEnabled(tenantId, "integrations");
        if (integrationsWasEnabled != integrationsNowEnabled) {
            integrationApiKeyLifecycleService.syncForIntegrationsModule(tenantId, integrationsNowEnabled);
        }
        auditService.log(tenantId, "PLATFORM_ADMIN", adminUid, "MODULES_ADMIN_UPDATED",
            Map.of("enabledModuleKeys", enabledModuleKeys, "integrationsEnabled", integrationsNowEnabled));
    }

    private boolean isModuleEnabled(String tenantId, String moduleKey) {
        return entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
            .anyMatch(e -> moduleKey.equals(e.getModuleKey()));
    }

    @Transactional(readOnly = true)
    public List<ModuleCatalogItemDto> getTenantModules(String tenantId) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        SubscriptionTier tier = tenant.getSubscriptionTier();

        Set<String> tierBaseline = tierBaselineRepository.findBySubscriptionTier(tier).stream()
            .map(b -> b.getModuleKey())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, TenantModuleEntitlement> entitlements = entitlementRepository.findByTenantId(tenantId).stream()
            .collect(Collectors.toMap(TenantModuleEntitlement::getModuleKey, e -> e, (a, b) -> a));

        return moduleRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(m -> {
            ModuleCatalogItemDto dto = new ModuleCatalogItemDto();
            dto.setModuleKey(m.getModuleKey());
            dto.setDisplayName(m.getDisplayName());
            dto.setDescription(ModuleCatalogDescriptions.forModule(m.getModuleKey(), m.getDisplayName()));
            dto.setRequired(m.isRequired());
            dto.setInTierBaseline(tierBaseline.contains(m.getModuleKey()));
            dto.setLocked(false);
            TenantModuleEntitlement ent = entitlements.get(m.getModuleKey());
            dto.setEnabled(ent != null && ent.isEnabled());
            if (ent != null && ent.getSource() != null) {
                dto.setEntitlementSource(ent.getSource().name());
            }
            return dto;
        }).toList();
    }

    public Set<String> computeEffectiveModules(SubscriptionTier tier, List<String> userSelected) {
        Set<String> effective = new LinkedHashSet<>();
        moduleRepository.findByActiveTrueOrderBySortOrderAsc().stream()
            .filter(AccessModule::isRequired)
            .map(AccessModule::getModuleKey)
            .forEach(effective::add);
        tierBaselineRepository.findBySubscriptionTier(tier).stream()
            .map(b -> b.getModuleKey())
            .forEach(effective::add);
        if (userSelected != null) {
            Set<String> validKeys = moduleRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(AccessModule::getModuleKey)
                .collect(Collectors.toSet());
            userSelected.stream().filter(validKeys::contains).forEach(effective::add);
        }
        return effective;
    }

    private void bumpSessionVersionForTenant(String tenantId) {
        provisioningService.bumpSessionVersion(tenantId);
    }
}
