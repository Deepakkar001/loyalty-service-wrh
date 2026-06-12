package com.loyaltyos.access.security;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.onboarding.security.TenantJwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("accessPermissionEvaluator")
public class AccessPermissionEvaluator {

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

    public boolean hasPermission(Authentication authentication, String permissionKey) {
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            return false;
        }
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        if (!accessProperties.getPermissions().isEnforce() && "TENANT_ADMIN".equals(TenantJwt.role(jwt))) {
            return true;
        }
        if (TenantJwt.isMerchant(jwt)) {
            return false;
        }
        String tenantId = TenantJwt.requireTenantId(jwt);
        String tenantUserId = TenantJwt.tenantUserId(jwt);
        if (tenantUserId == null || tenantUserId.isBlank()) {
            String email = TenantJwt.email(jwt);
            tenantUserId = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
                .map(u -> u.getUserId())
                .orElse(null);
        }
        if (tenantUserId == null) {
            // Legacy JWTs issued before tenant_user cutover: allow to avoid breaking existing sessions.
            return "TENANT_ADMIN".equals(TenantJwt.role(jwt));
        }
        return accessResolutionService.hasPermission(tenantId, tenantUserId, permissionKey);
    }
}
