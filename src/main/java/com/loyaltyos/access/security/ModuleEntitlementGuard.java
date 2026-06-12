package com.loyaltyos.access.security;

import com.loyaltyos.access.exception.ModuleNotEntitledException;
import com.loyaltyos.access.service.AccessResolutionService;
import org.springframework.stereotype.Component;

@Component("moduleEntitlementGuard")
public class ModuleEntitlementGuard {

    private final AccessResolutionService accessResolutionService;

    public ModuleEntitlementGuard(AccessResolutionService accessResolutionService) {
        this.accessResolutionService = accessResolutionService;
    }

    public void requireModule(String tenantId, String moduleKey) {
        if (!accessResolutionService.isModuleEntitled(tenantId, moduleKey)) {
            throw new ModuleNotEntitledException(moduleKey);
        }
    }
}
