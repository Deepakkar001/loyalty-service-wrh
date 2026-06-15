package com.loyaltyos.access.security;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.exception.ModuleNotEntitledException;
import com.loyaltyos.access.exception.PermissionDeniedException;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.onboarding.security.TenantJwt;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("moduleEntitlementGuard")
public class ModuleEntitlementGuard {

    private final AccessProperties accessProperties;
    private final AccessResolutionService accessResolutionService;
    private final TenantUserRepository userRepository;

    public ModuleEntitlementGuard(
        AccessProperties accessProperties,
        AccessResolutionService accessResolutionService,
        TenantUserRepository userRepository
    ) {
        this.accessProperties = accessProperties;
        this.accessResolutionService = accessResolutionService;
        this.userRepository = userRepository;
    }

    public void requireModule(String tenantId, String moduleKey) {
        if (!accessResolutionService.isModuleEntitled(tenantId, moduleKey)) {
            throw new ModuleNotEntitledException(moduleKey);
        }
    }

    public void requireModuleAccess(Jwt jwt, String moduleKey, String httpMethod, String path) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        requireModule(tenantId, moduleKey);
        if (!accessProperties.getPermissions().isEnforce()) {
            return;
        }

        String tenantUserId = resolveTenantUserId(jwt);
        if (tenantUserId == null) {
            throw new PermissionDeniedException(moduleKey + ".view");
        }

        String permissionKey = TenantModulePermissionResolver.resolve(moduleKey, httpMethod, path);
        if (!accessResolutionService.hasPermission(tenantId, tenantUserId, permissionKey)) {
            throw new PermissionDeniedException(permissionKey);
        }
    }

    private String resolveTenantUserId(Jwt jwt) {
        String tenantUserId = TenantJwt.tenantUserId(jwt);
        if (tenantUserId != null && !tenantUserId.isBlank()) {
            return tenantUserId;
        }
        String tenantId = TenantJwt.tenantId(jwt);
        String email = TenantJwt.email(jwt);
        if (tenantId == null || email == null) {
            return null;
        }
        return userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
            .map(u -> u.getUserId())
            .orElse(null);
    }
}
