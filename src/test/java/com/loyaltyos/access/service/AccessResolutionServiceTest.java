package com.loyaltyos.access.service;

import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.entity.AccessModuleAction;
import com.loyaltyos.access.entity.PrivilegeGrant;
import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.entity.TenantRole;
import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.entity.TenantUserRole;
import com.loyaltyos.access.enums.GrantEffect;
import com.loyaltyos.access.enums.GrantSubjectType;
import com.loyaltyos.access.enums.TenantUserStatus;
import com.loyaltyos.access.repository.AccessModuleActionRepository;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.AccessNavItemRepository;
import com.loyaltyos.access.repository.PrivilegeGrantRepository;
import com.loyaltyos.access.repository.TenantModuleEntitlementRepository;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.repository.TenantUserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessResolutionServiceTest {

    @Mock private AccessProperties accessProperties;
    @Mock private TenantUserRepository userRepository;
    @Mock private TenantUserRoleRepository userRoleRepository;
    @Mock private TenantRoleRepository roleRepository;
    @Mock private TenantModuleEntitlementRepository entitlementRepository;
    @Mock private PrivilegeGrantRepository grantRepository;
    @Mock private AccessModuleRepository moduleRepository;
    @Mock private AccessNavItemRepository navItemRepository;
    @Mock private AccessModuleActionRepository moduleActionRepository;

    private AccessResolutionService service;

    @BeforeEach
    void setUp() {
        service = new AccessResolutionService(
            accessProperties, userRepository, userRoleRepository, roleRepository,
            entitlementRepository, grantRepository, moduleRepository, navItemRepository, moduleActionRepository
        );
    }

    @Test
    void resolveEffectivePermissions_appliesUserDeny() {
        String tenantId = "t1";
        String userId = "u1";
        String roleId = "r1";
        Set<String> candidates = Set.of("campaigns.view", "campaigns.create");

        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(new TenantUserRole(userId, roleId, tenantId)));
        PrivilegeGrant roleGrant = new PrivilegeGrant();
        roleGrant.setPermissionKey("campaigns.view");
        roleGrant.setEffect(GrantEffect.GRANT);
        PrivilegeGrant roleGrant2 = new PrivilegeGrant();
        roleGrant2.setPermissionKey("campaigns.create");
        roleGrant2.setEffect(GrantEffect.GRANT);
        when(grantRepository.findByTenantIdAndSubjectTypeAndSubjectIdIn(
            eq(tenantId), eq(GrantSubjectType.ROLE), eq(List.of(roleId))))
            .thenReturn(List.of(roleGrant, roleGrant2));
        when(grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
            eq(tenantId), eq(GrantSubjectType.USER), eq(userId)))
            .thenReturn(List.of(deny("campaigns.create")));
        when(roleRepository.findByTenantIdAndSystemTrue(tenantId)).thenReturn(Optional.empty());

        Set<String> effective = service.resolveEffectivePermissions(tenantId, userId, candidates);
        assertTrue(effective.contains("campaigns.view"));
        assertFalse(effective.contains("campaigns.create"));
    }

    @Test
    void legacyPrimaryAdminGetsAllCandidatePermissionsWhenNoGrants() {
        String tenantId = "t1";
        String userId = "u1";
        Set<String> candidates = Set.of("campaigns.view", "integrations.view");

        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());
        when(grantRepository.findByTenantIdAndSubjectTypeAndSubjectIdIn(any(), any(), any())).thenReturn(List.of());
        when(grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(any(), any(), any())).thenReturn(List.of());
        TenantRole systemRole = new TenantRole();
        systemRole.setSystem(true);
        when(roleRepository.findByTenantIdAndSystemTrue(tenantId)).thenReturn(Optional.of(systemRole));

        Set<String> effective = service.resolveEffectivePermissions(tenantId, userId, candidates);
        assertTrue(effective.containsAll(candidates));
    }

    private static PrivilegeGrant deny(String key) {
        PrivilegeGrant g = new PrivilegeGrant();
        g.setPermissionKey(key);
        g.setEffect(GrantEffect.DENY);
        return g;
    }
}
