package com.loyaltyos.access.security;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.onboarding.security.TenantJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component("accessPermissionEvaluator")
public class AccessPermissionEvaluator implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AccessPermissionEvaluator.class);

    private final AccessProperties accessProperties;
    private final AccessResolutionService accessResolutionService;
    private final TenantUserRepository userRepository;

    public AccessPermissionEvaluator(
        AccessProperties accessProperties,
        AccessResolutionService accessResolutionService,
        TenantUserRepository userRepository
    ) {
        this.accessProperties = accessProperties;
        this.accessResolutionService = accessResolutionService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return evaluate(authentication, permission);
    }

    @Override
    public boolean hasPermission(
        Authentication authentication,
        Serializable targetId,
        String targetType,
        Object permission
    ) {
        return evaluate(authentication, permission);
    }

    private boolean evaluate(Authentication authentication, Object permission) {
        if (permission == null) {
            return false;
        }
        String permissionKey = String.valueOf(permission).trim();
        if (permissionKey.isEmpty()) {
            return false;
        }

        Jwt jwt = TenantJwt.resolve(authentication);
        if (jwt == null) {
            return false;
        }
        if ("admin".equals(TenantJwt.type(jwt))) {
            return false;
        }
        if (TenantJwt.isMerchant(jwt)) {
            return false;
        }

        try {
            if (!accessProperties.getPermissions().isEnforce() && "TENANT_ADMIN".equals(TenantJwt.role(jwt))) {
                return true;
            }

            String tenantId = TenantJwt.tenantId(jwt);
            if (tenantId == null || tenantId.isBlank()) {
                return false;
            }
            tenantId = tenantId.trim();

            String tenantUserId = TenantJwt.tenantUserId(jwt);
            if (tenantUserId == null || tenantUserId.isBlank()) {
                String email = TenantJwt.email(jwt);
                tenantUserId = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
                    .map(u -> u.getUserId())
                    .orElse(null);
            }
            if (tenantUserId == null) {
                return "TENANT_ADMIN".equals(TenantJwt.role(jwt));
            }
            return accessResolutionService.hasPermission(tenantId, tenantUserId, permissionKey);
        } catch (RuntimeException ex) {
            log.warn("Permission check failed for {}: {}", permissionKey, ex.getMessage());
            return false;
        }
    }
}
