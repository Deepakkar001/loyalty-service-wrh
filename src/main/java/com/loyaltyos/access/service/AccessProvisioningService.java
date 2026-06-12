package com.loyaltyos.access.service;

import com.loyaltyos.access.entity.PrivilegeGrant;
import com.loyaltyos.access.entity.TenantModuleEntitlement;
import com.loyaltyos.access.entity.TenantRole;
import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.entity.TenantUserRole;
import com.loyaltyos.access.enums.EntitlementSource;
import com.loyaltyos.access.enums.GrantEffect;
import com.loyaltyos.access.enums.GrantSubjectType;
import com.loyaltyos.access.enums.TenantUserStatus;
import com.loyaltyos.access.repository.AccessModuleActionRepository;
import com.loyaltyos.access.repository.AccessModuleRepository;
import com.loyaltyos.access.repository.PrivilegeGrantRepository;
import com.loyaltyos.access.repository.TenantModuleEntitlementRepository;
import com.loyaltyos.access.repository.TenantRoleRepository;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.repository.TenantUserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AccessProvisioningService {

    public static final String PRIMARY_ADMIN_ROLE_NAME = "Primary Administrator";

    private final TenantUserRepository userRepository;
    private final TenantRoleRepository roleRepository;
    private final TenantUserRoleRepository userRoleRepository;
    private final TenantModuleEntitlementRepository entitlementRepository;
    private final PrivilegeGrantRepository grantRepository;
    private final AccessModuleRepository moduleRepository;
    private final AccessModuleActionRepository moduleActionRepository;

    public AccessProvisioningService(
        TenantUserRepository userRepository,
        TenantRoleRepository roleRepository,
        TenantUserRoleRepository userRoleRepository,
        TenantModuleEntitlementRepository entitlementRepository,
        PrivilegeGrantRepository grantRepository,
        AccessModuleRepository moduleRepository,
        AccessModuleActionRepository moduleActionRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.entitlementRepository = entitlementRepository;
        this.grantRepository = grantRepository;
        this.moduleRepository = moduleRepository;
        this.moduleActionRepository = moduleActionRepository;
    }

    @Transactional
    public TenantUser provisionPrimaryAdmin(String tenantId, String email, String passwordHash, String fullName) {
        String normalizedEmail = email.toLowerCase().trim();
        var existing = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, normalizedEmail);
        if (existing.isPresent()) {
            TenantUser user = existing.get();
            if (passwordHash != null && !passwordHash.isBlank()
                && (user.getPasswordHash() == null || !user.getPasswordHash().equals(passwordHash))) {
                user.setPasswordHash(passwordHash);
                userRepository.save(user);
            }
            return user;
        }

        String userId = UUID.randomUUID().toString();
        TenantUser user = new TenantUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHash);
        user.setFullName(fullName);
        user.setStatus(TenantUserStatus.ACTIVE);
        user.setSessionVersion(1);
        userRepository.save(user);

        TenantRole role = provisionSystemRole(tenantId);
        userRoleRepository.save(new TenantUserRole(userId, role.getRoleId(), tenantId));
        return user;
    }

    @Transactional
    public TenantRole provisionSystemRole(String tenantId) {
        return roleRepository.findByTenantIdAndSystemTrue(tenantId)
            .orElseGet(() -> {
                TenantRole role = new TenantRole();
                role.setRoleId(UUID.randomUUID().toString());
                role.setTenantId(tenantId);
                role.setRoleName(PRIMARY_ADMIN_ROLE_NAME);
                role.setDescription("Full access to all entitled modules");
                role.setSystem(true);
                return roleRepository.save(role);
            });
    }

    @Transactional
    public void grantAllEntitledPermissions(String tenantId, String roleId) {
        List<String> moduleKeys = entitlementRepository.findByTenantIdAndEnabledTrue(tenantId).stream()
            .map(TenantModuleEntitlement::getModuleKey)
            .toList();
        if (moduleKeys.isEmpty()) {
            return;
        }
        moduleActionRepository.findByModuleKeyIn(moduleKeys).forEach(ma -> {
            if (!grantRepository.findByTenantIdAndSubjectTypeAndSubjectId(
                tenantId, GrantSubjectType.ROLE, roleId).stream()
                .anyMatch(g -> g.getPermissionKey().equals(ma.getPermissionKey()))) {
                PrivilegeGrant grant = new PrivilegeGrant();
                grant.setTenantId(tenantId);
                grant.setSubjectType(GrantSubjectType.ROLE);
                grant.setSubjectId(roleId);
                grant.setPermissionKey(ma.getPermissionKey());
                grant.setEffect(GrantEffect.GRANT);
                grantRepository.save(grant);
            }
        });
    }

    @Transactional
    public void provisionFullEntitlements(String tenantId, EntitlementSource source, String enabledBy) {
        moduleRepository.findByActiveTrueOrderBySortOrderAsc().forEach(module -> {
            TenantModuleEntitlement ent = new TenantModuleEntitlement();
            ent.setTenantId(tenantId);
            ent.setModuleKey(module.getModuleKey());
            ent.setEnabled(true);
            ent.setSource(source);
            ent.setEnabledBy(enabledBy);
            entitlementRepository.save(ent);
        });
    }

    @Transactional
    public void syncPrimaryAdminGrants(String tenantId) {
        roleRepository.findByTenantIdAndSystemTrue(tenantId).ifPresent(role -> {
            grantRepository.deleteByTenantIdAndSubjectTypeAndSubjectId(
                tenantId, GrantSubjectType.ROLE, role.getRoleId());
            grantAllEntitledPermissions(tenantId, role.getRoleId());
        });
    }

    public boolean hasEntitlements(String tenantId) {
        return entitlementRepository.existsByTenantId(tenantId);
    }

    @Transactional
    public void bumpSessionVersion(String tenantId) {
        userRepository.findByTenantId(tenantId).forEach(user -> {
            user.setSessionVersion(user.getSessionVersion() + 1);
            userRepository.save(user);
        });
    }
}
